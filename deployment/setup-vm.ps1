$ErrorActionPreference = 'Stop'

# Prepare only the new, blank 128 GiB data disk attached to the Azure VM.
$dataDisk = Get-Disk -Number 2
if ($dataDisk.Size -lt 120GB) {
    throw 'Disk 2 is smaller than the expected 128 GiB data disk; refusing to initialize it.'
}
if ($dataDisk.PartitionStyle -eq 'RAW') {
    Initialize-Disk -Number 2 -PartitionStyle GPT | Out-Null
    $partition = New-Partition -DiskNumber 2 -UseMaximumSize -DriveLetter F
    Format-Volume -Partition $partition -FileSystem NTFS -NewFileSystemLabel 'HotelData' -Confirm:$false | Out-Null
} elseif ($dataDisk.PartitionStyle -ne 'GPT' -or -not (Get-Volume -DriveLetter F -ErrorAction SilentlyContinue)) {
    throw 'Disk 2 is not the expected blank disk mounted as F:; refusing to continue.'
}

$installers = 'F:\Installers'
New-Item -ItemType Directory -Force -Path $installers, 'F:\Java8' | Out-Null

# Download the current Eclipse Temurin Java 8 JDK and verify its published SHA-256.
$javaAssets = Invoke-RestMethod -Uri 'https://api.adoptium.net/v3/assets/latest/8/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse'
$javaPackage = $javaAssets[0].binary.package
$javaZip = Join-Path $installers 'temurin8.zip'
Invoke-WebRequest -Uri $javaPackage.link -OutFile $javaZip
if ((Get-FileHash $javaZip -Algorithm SHA256).Hash -ne $javaPackage.checksum) {
    throw 'Temurin JDK SHA-256 verification failed.'
}
Expand-Archive -Path $javaZip -DestinationPath 'F:\Java8' -Force
$jdk = Get-ChildItem 'F:\Java8' -Directory | Select-Object -First 1
if (-not $jdk) { throw 'Temurin JDK extraction failed.' }

# Download Tomcat 9.0.123 and verify Apache's published SHA-512.
$tomcatVersion = '9.0.123'
$tomcatFile = "apache-tomcat-$tomcatVersion-windows-x64.zip"
$tomcatBase = "https://dlcdn.apache.org/tomcat/tomcat-9/v$tomcatVersion/bin"
$tomcatZip = Join-Path $installers 'tomcat9.zip'
Invoke-WebRequest -Uri "$tomcatBase/$tomcatFile" -OutFile $tomcatZip
Invoke-WebRequest -Uri "$tomcatBase/$tomcatFile.sha512" -OutFile "$tomcatZip.sha512"
$expectedHash = [regex]::Match((Get-Content -Raw "$tomcatZip.sha512"), '[0-9a-fA-F]{128}').Value
if (-not $expectedHash -or (Get-FileHash $tomcatZip -Algorithm SHA512).Hash -ne $expectedHash) {
    throw 'Tomcat SHA-512 verification failed.'
}

$tomcatHome = 'F:\Tomcat9'
if (-not (Test-Path $tomcatHome)) {
    $extractDir = Join-Path $installers 'tomcat-extract'
    Expand-Archive -Path $tomcatZip -DestinationPath $extractDir -Force
    Move-Item -LiteralPath (Join-Path $extractDir "apache-tomcat-$tomcatVersion") -Destination $tomcatHome
}

[Environment]::SetEnvironmentVariable('JAVA_HOME', $jdk.FullName, 'Machine')
[Environment]::SetEnvironmentVariable('CATALINA_HOME', $tomcatHome, 'Machine')
$env:JAVA_HOME = $jdk.FullName
$env:CATALINA_HOME = $tomcatHome
$env:Path = "$($jdk.FullName)\bin;$env:Path"

$serverXmlPath = Join-Path $tomcatHome 'conf\server.xml'
$serverXml = Get-Content -LiteralPath $serverXmlPath -Raw
if ($serverXml.Contains('<Connector port="8080"')) {
    $serverXml = $serverXml.Replace('<Connector port="8080"', '<Connector port="8081"')
    Set-Content -LiteralPath $serverXmlPath -Value $serverXml -Encoding UTF8
} elseif (-not $serverXml.Contains('<Connector port="8081"')) {
    throw 'Could not find Tomcat HTTP connector in server.xml.'
}

# The deployment commit is pinned so the VM receives the reviewed WAR and schema.
$release = '229a8d1'
$rawBase = "https://raw.githubusercontent.com/devopscloud90/HotelBilling/$release"
New-Item -ItemType Directory -Force -Path 'F:\HotelBilling\db' | Out-Null
Invoke-WebRequest -Uri "$rawBase/HotelBilling.war" -OutFile "$tomcatHome\webapps\HotelBilling.war"
Invoke-WebRequest -Uri "$rawBase/db/schema-oracle.sql" -OutFile 'F:\HotelBilling\db\schema-oracle.sql'

if (-not (Get-Service -Name HotelBillingTomcat -ErrorAction SilentlyContinue)) {
    Push-Location "$tomcatHome\bin"
    try {
        & .\service.bat install HotelBillingTomcat
        if ($LASTEXITCODE -ne 0) { throw 'Tomcat Windows service installation failed.' }
    } finally {
        Pop-Location
    }
}
Set-Service -Name HotelBillingTomcat -StartupType Automatic
Start-Service -Name HotelBillingTomcat
Write-Output 'Java 8 and Tomcat 9 are installed. HotelBilling.war and schema-oracle.sql are staged. Tomcat is running on local-only port 8081.'

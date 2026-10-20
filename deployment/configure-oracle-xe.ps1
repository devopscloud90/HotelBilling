$ErrorActionPreference = 'Stop'

foreach ($serviceName in 'OracleServiceXE', 'OracleXETNSListener') {
    $service = Get-Service -Name $serviceName -ErrorAction Stop
    if ($service.Status -ne 'Running') { throw "$serviceName is not running." }
}

$oracleRoots = 'HKLM:\SOFTWARE\ORACLE', 'HKLM:\SOFTWARE\WOW6432Node\ORACLE'
$oracleHomes = foreach ($root in $oracleRoots) {
    Get-ChildItem $root -ErrorAction SilentlyContinue | ForEach-Object {
        (Get-ItemProperty $_.PSPath -ErrorAction SilentlyContinue).ORACLE_HOME
    }
}
$oracleHome = $oracleHomes | Where-Object { $_ -and (Test-Path (Join-Path $_ 'bin\sqlplus.exe')) } | Select-Object -First 1
if (-not $oracleHome) { throw 'Could not find Oracle XE sqlplus.exe in the Oracle registry entries.' }
$sqlplus = Join-Path $oracleHome 'bin\sqlplus.exe'
$env:ORACLE_HOME = $oracleHome
$env:Path = "$oracleHome\bin;$env:Path"

# Generate the application password on the VM; it is not stored in source control or printed.
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$secretBytes = New-Object byte[] 15
try { $rng.GetBytes($secretBytes) } finally { $rng.Dispose() }
$appPassword = [BitConverter]::ToString($secretBytes).Replace('-', '')
$userSql = 'F:\HotelBilling\create-app-user.sql'
$schemaSql = 'F:\HotelBilling\apply-schema.sql'

try {
    @"
WHENEVER SQLERROR EXIT SQL.SQLCODE
DECLARE
  user_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO user_count FROM dba_users WHERE username = 'HOTELBILLING';
  IF user_count = 0 THEN
    EXECUTE IMMEDIATE 'CREATE USER HOTELBILLING IDENTIFIED BY "$appPassword" DEFAULT TABLESPACE USERS TEMPORARY TABLESPACE TEMP QUOTA UNLIMITED ON USERS';
  ELSE
    EXECUTE IMMEDIATE 'ALTER USER HOTELBILLING IDENTIFIED BY "$appPassword"';
  END IF;
END;
/
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE INDEX, CREATE VIEW, CREATE PROCEDURE TO HOTELBILLING;
EXIT
"@ | Set-Content -LiteralPath $userSql -Encoding ASCII

    & $sqlplus -L -S '/ as sysdba' "@$userSql"
    if ($LASTEXITCODE -ne 0) { throw 'Oracle OS-authenticated SYSDBA user setup failed.' }

    @"
WHENEVER SQLERROR EXIT SQL.SQLCODE
@F:\HotelBilling\db\schema-oracle.sql
EXIT
"@ | Set-Content -LiteralPath $schemaSql -Encoding ASCII

    & $sqlplus -L -S "hotelbilling/$appPassword@//localhost:1521/XE" "@$schemaSql"
    if ($LASTEXITCODE -ne 0) { throw 'HotelBilling schema import failed.' }

    $tomcatExe = 'F:\Tomcat9\bin\tomcat9.exe'
    $serviceEnvironment = "HOTEL_DB_URL=jdbc:oracle:thin:@localhost:1521:XE;HOTEL_DB_USER=hotelbilling;HOTEL_DB_PASSWORD=$appPassword"
    & $tomcatExe '//US//HotelBillingTomcat' "++Environment=$serviceEnvironment"
    if ($LASTEXITCODE -ne 0) { throw 'Could not configure the Tomcat service database settings.' }
    Restart-Service -Name HotelBillingTomcat -Force
    Start-Sleep -Seconds 8
    $response = Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8080/HotelBilling/setup' -TimeoutSec 30
    Write-Output "Oracle XE schema initialized. Tomcat setup endpoint returned HTTP $([int]$response.StatusCode). Open http://localhost:8080/HotelBilling/setup inside the VM to create the first admin account."
} finally {
    Remove-Item -LiteralPath $userSql, $schemaSql -Force -ErrorAction SilentlyContinue
    [Array]::Clear($secretBytes, 0, $secretBytes.Length)
    $appPassword = $null
}

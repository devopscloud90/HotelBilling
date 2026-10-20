$ErrorActionPreference = 'Stop'

$serverXmlPath = 'F:\Tomcat9\conf\server.xml'
$serverXml = Get-Content -LiteralPath $serverXmlPath -Raw
if ($serverXml.Contains('<Connector port="8080"')) {
    $serverXml = $serverXml.Replace('<Connector port="8080"', '<Connector port="8081"')
    Set-Content -LiteralPath $serverXmlPath -Value $serverXml -Encoding UTF8
} elseif (-not $serverXml.Contains('<Connector port="8081"')) {
    throw 'Could not find Tomcat HTTP connector in server.xml.'
}

Restart-Service -Name HotelBillingTomcat -Force
$listener = $null
for ($attempt = 0; $attempt -lt 20 -and -not $listener; $attempt++) {
    Start-Sleep -Seconds 2
    $listener = Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction SilentlyContinue
}
if (-not $listener) { throw 'Tomcat did not bind to local port 8081.' }

$response = Invoke-WebRequest -UseBasicParsing -Proxy $null -Uri 'http://127.0.0.1:8081/HotelBilling/setup' -TimeoutSec 30
Write-Output "Tomcat is listening on port 8081. HotelBilling setup returned HTTP $([int]$response.StatusCode). Open http://localhost:8081/HotelBilling/setup inside the VM."

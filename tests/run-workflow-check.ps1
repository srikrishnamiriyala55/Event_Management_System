param(
    [string]$TomcatHome = 'C:/Program Files/Apache Software Foundation/Tomcat 9.0',
    [int]$Port = 19082
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$testRoot = Join-Path $projectRoot 'target/workflow-check'
$war = Join-Path $projectRoot 'target/campus-events.war'
if (!(Test-Path -LiteralPath $war)) {
    throw 'Build the application with mvn verify first.'
}
$previousUrl = $env:DB_URL
$previousOrganizerPassword = $env:DEFAULT_ORGANIZER_PASSWORD
$previousOrganizerId = $env:DEFAULT_ORGANIZER_ID
$serverProcess = $null
$env:DB_URL = 'jdbc:mysql://localhost:3306/campus_events_qa_' +
    (Get-Date -Format 'yyyyMMddHHmmssfff') +
    '?createDatabaseIfNotExist=true&serverTimezone=Asia/Kolkata'
$env:DEFAULT_ORGANIZER_PASSWORD = 'Admin@123'
$env:DEFAULT_ORGANIZER_ID = 'ORG001'
$databaseProbe = Join-Path $PSScriptRoot 'DatabaseProbe.java'
$databaseClasspath = "$projectRoot/target/classes;$projectRoot/target/campus-events/WEB-INF/lib/*"

try {
    New-Item -ItemType Directory -Path "$testRoot/conf" -Force | Out-Null
    Copy-Item -LiteralPath "$TomcatHome/conf/web.xml" -Destination "$testRoot/conf/web.xml"
    $contextPath = [System.Security.SecurityElement]::Escape($war)
    @"
<?xml version="1.0" encoding="UTF-8"?>
<Server port="-1">
    <Service name="WorkflowCheck">
        <Connector port="$Port" address="127.0.0.1" protocol="HTTP/1.1" />
        <Engine name="WorkflowCheck" defaultHost="localhost">
            <Host name="localhost" appBase="webapps" autoDeploy="false" deployOnStartup="false">
                <Context path="/campus-events" docBase="$contextPath" />
            </Host>
        </Engine>
    </Service>
</Server>
"@ | Set-Content -LiteralPath "$testRoot/conf/server.xml" -Encoding utf8
    $javaArguments = @(
        "`"-Dcatalina.home=$TomcatHome`"",
        "`"-Dcatalina.base=$testRoot`"",
        '-cp', "`"$TomcatHome/bin/bootstrap.jar;$TomcatHome/bin/tomcat-juli.jar`"",
        'org.apache.catalina.startup.Bootstrap', 'start'
    )
    $serverProcess = Start-Process -FilePath (Get-Command java).Source -ArgumentList $javaArguments `
        -PassThru -WindowStyle Hidden -RedirectStandardOutput "$testRoot/stdout.log" `
        -RedirectStandardError "$testRoot/stderr.log"
    $baseUrl = "http://127.0.0.1:$Port/campus-events"
    $ready = $false
    for ($attempt = 0; $attempt -lt 45; $attempt++) {
        if ($serverProcess.HasExited) { throw "Test Tomcat exited; see $testRoot/stderr.log" }
        try {
            $page = Invoke-WebRequest -Uri "$baseUrl/login" -TimeoutSec 2
            if ($page.StatusCode -eq 200) { $ready = $true; break }
        } catch { Start-Sleep -Seconds 1 }
    }
    if (!$ready) { throw "Test Tomcat did not become ready; see $testRoot/stderr.log" }
    & python "$PSScriptRoot/workflows.py" $baseUrl
    if ($LASTEXITCODE -ne 0) { throw 'Workflow checks failed.' }
    & java -cp $databaseClasspath $databaseProbe
    if ($LASTEXITCODE -ne 0) { throw 'Database checks failed.' }
} finally {
    if ($serverProcess -and !$serverProcess.HasExited) {
        Stop-Process -Id $serverProcess.Id
        $serverProcess.WaitForExit()
    }
    & java -cp $databaseClasspath $databaseProbe cleanup
    $env:DB_URL = $previousUrl
    $env:DEFAULT_ORGANIZER_PASSWORD = $previousOrganizerPassword
    $env:DEFAULT_ORGANIZER_ID = $previousOrganizerId
}

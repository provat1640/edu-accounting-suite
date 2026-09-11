$ErrorActionPreference = "Stop"

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw "Maven was not found. Install Maven and add its bin directory to PATH."
}

if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
    throw "jpackage was not found. Install JDK 17 or newer and add its bin directory to PATH."
}

mvn clean package

$jar = Join-Path $PSScriptRoot "target\EduAccountingSuite-1.0-SNAPSHOT-all.jar"
if (-not (Test-Path $jar)) {
    throw "The shaded application JAR was not created: $jar"
}

$dist = Join-Path $PSScriptRoot "dist"
if (Test-Path $dist) {
    Remove-Item -Path $dist -Recurse -Force
}
New-Item -ItemType Directory -Path $dist | Out-Null

jpackage `
    --type exe `
    --name EduAccountingSuite `
    --app-version 1.0.0 `
    --vendor "EduAccountingSuite" `
    --description "Accounting-cycle learning software for students" `
    --input (Split-Path $jar) `
    --main-jar (Split-Path $jar -Leaf) `
    --main-class com.accounting.Main `
    --dest $dist `
    --win-shortcut `
    --win-menu `
    --win-menu-group "EduAccountingSuite"

Write-Host "Installer created: $dist\EduAccountingSuite-1.0.0.exe"

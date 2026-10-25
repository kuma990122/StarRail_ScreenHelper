$ErrorActionPreference = 'Stop'
$taskProjectRoot = Split-Path $PSScriptRoot -Parent
$taskJdkDirectory = Get-ChildItem -LiteralPath (Join-Path $taskProjectRoot '.tooling/jdk') -Directory | Select-Object -First 1
if (-not $taskJdkDirectory) { throw 'Install a JDK into .tooling/jdk first.' }
$env:JAVA_HOME = $taskJdkDirectory.FullName
$env:ANDROID_HOME = Join-Path $taskProjectRoot '.tooling/android-sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:ANDROID_USER_HOME = Join-Path $taskProjectRoot '.tooling/android-home'
$env:ANDROID_EMULATOR_HOME = $env:ANDROID_USER_HOME
$env:ANDROID_AVD_HOME = Join-Path $taskProjectRoot '.tooling/avd'
$env:GRADLE_USER_HOME = Join-Path $taskProjectRoot '.tooling/gradle-cache'
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\emulator;$env:PATH"
New-Item -ItemType Directory -Path $env:ANDROID_USER_HOME,$env:ANDROID_AVD_HOME,$env:GRADLE_USER_HOME -Force | Out-Null

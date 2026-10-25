param(
    [ValidateSet('Wrapper','SetupSdk','Dependencies','Build','Test','Lint','Verify','Install','Run','Devices','Emulator','Instrument','Diagnostics','DeviceLogs','Screenshots','PageScreenshots','BuildStatus','StopEmulator')]
    [string]$Task = 'Build',
    [switch]$Headless,
    [string]$Device
)
. "$PSScriptRoot/env.ps1"
Set-Location -LiteralPath $taskProjectRoot
$taskSdkManager = Join-Path $env:ANDROID_HOME 'cmdline-tools/stable/bin/sdkmanager.bat'
$taskAvdManager = Join-Path $env:ANDROID_HOME 'cmdline-tools/stable/bin/avdmanager.bat'
$taskAdb = Join-Path $env:ANDROID_HOME 'platform-tools/adb.exe'
New-Item -ItemType Directory -Path tmp -Force | Out-Null
function Invoke-DeviceTests {
    & ./gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest --max-workers=1 --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Test APK build failed.' }
    & $taskAdb install -r app/build/outputs/apk/debug/app-debug.apk
    if ($LASTEXITCODE -ne 0) { throw 'App installation failed.' }
    & $taskAdb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
    if ($LASTEXITCODE -ne 0) { throw 'Test installation failed.' }
    $taskTestOutput = @(& $taskAdb shell am instrument -w -r com.screenhelper.test/androidx.test.runner.AndroidJUnitRunner)
    $taskTestOutput | Tee-Object -FilePath tmp/instrumentation-result.txt
    if ($LASTEXITCODE -ne 0 -or -not ($taskTestOutput -match '^OK \(')) { throw 'Device tests did not finish successfully. See tmp/instrumentation-result.txt.' }
}
switch ($Task) {
    'Wrapper' {
        New-Item -ItemType Directory -Path .tooling/wrapper-bootstrap -Force | Out-Null
        Set-Content -LiteralPath .tooling/wrapper-bootstrap/settings.gradle -Value "rootProject.name = 'wrapper-bootstrap'" -Encoding ASCII
        & ./.tooling/gradle-8.9/bin/gradle.bat -p .tooling/wrapper-bootstrap wrapper --gradle-version 8.9 --distribution-type bin --gradle-distribution-sha256-sum d725d707bfabd4dfdc958c624003b3c80accc03f7037b5122c4b1d0ef15cecab --no-validate-url --offline --console=plain
        if ($LASTEXITCODE -ne 0) { throw 'Wrapper generation failed.' }
        Copy-Item -LiteralPath .tooling/wrapper-bootstrap/gradlew,.tooling/wrapper-bootstrap/gradlew.bat -Destination . -Force
        Copy-Item -LiteralPath .tooling/wrapper-bootstrap/gradle -Destination . -Recurse -Force
        python -c "import hashlib,pathlib; n=int(hashlib.md5(b'https://services.gradle.org/distributions/gradle-8.9-bin.zip').hexdigest(),16); s=''; chars='0123456789abcdefghijklmnopqrstuvwxyz'; exec('while n: s=chars[n%36]+s; n//=36'); p=pathlib.Path('.tooling/gradle-cache/wrapper/dists/gradle-8.9-bin')/s; p.mkdir(parents=True,exist_ok=True); import shutil; shutil.copyfile('.tooling/downloads/gradle-mirror.zip',str(p/'gradle-8.9-bin.zip'))"
    }
    'SetupSdk' {
        1..80 | ForEach-Object { 'y' } | & $taskSdkManager "--sdk_root=$env:ANDROID_HOME" --licenses
        & $taskSdkManager "--sdk_root=$env:ANDROID_HOME" 'platform-tools' 'platforms;android-35' 'build-tools;35.0.0' 'emulator' 'system-images;android-35;default;x86_64'
        if ($LASTEXITCODE -ne 0) { throw 'SDK installation failed.' }
        $taskSdkPath = $env:ANDROID_HOME.Replace('\','/')
        [System.IO.File]::WriteAllText((Join-Path $taskProjectRoot 'local.properties'), "sdk.dir=$taskSdkPath`n", (New-Object System.Text.UTF8Encoding($false)))
    }
    'Dependencies' { & ./gradlew.bat :app:dependencies --configuration debugRuntimeClasspath --console=plain }
    'Build' { & ./gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest --max-workers=1 --stacktrace --console=plain }
    'Test' { & ./gradlew.bat :app:testDebugUnitTest --console=plain }
    'Lint' { & ./gradlew.bat :app:lintDebug --console=plain }
    'Verify' {
        & ./gradlew.bat :app:testDebugUnitTest :app:lintDebug --max-workers=1 --console=plain
        if ($LASTEXITCODE -ne 0) { throw 'Unit tests or lint failed.' }
        Invoke-DeviceTests
    }
    'Install' { & ./gradlew.bat :app:installDebug :app:installDebugAndroidTest --console=plain }
    'Devices' { & $taskAdb devices -l }
    'DeviceLogs' {
        & $taskAdb logcat -d -t 3000 | Set-Content tmp/device-logcat.txt -Encoding UTF8
        & $taskAdb shell dumpsys alarm | Set-Content tmp/device-alarms.txt -Encoding UTF8
        & $taskAdb shell appops get com.screenhelper SCHEDULE_EXACT_ALARM
    }
    'BuildStatus' {
        & (Join-Path $env:JAVA_HOME 'bin/jps.exe') -lv
        $taskDaemons = Get-Process java -ErrorAction SilentlyContinue | Where-Object { $_.WorkingSet64 -gt 400000000 }
        foreach ($taskDaemon in $taskDaemons) { & (Join-Path $env:JAVA_HOME 'bin/jstack.exe') $taskDaemon.Id | Set-Content ("tmp/java-" + $taskDaemon.Id + "-threads.txt") -Encoding UTF8 }
    }
    'Run' {
        & ./gradlew.bat :app:assembleDebug --console=plain
        if ($LASTEXITCODE -ne 0) { throw 'Build failed. Resolve the compiler errors before launching.' }
        $taskDeviceLines = @(& $taskAdb devices | Where-Object { $_ -match '^([^\s]+)\s+device$' })
        if ($Device) {
            $taskSerial = $Device
        } elseif ($taskDeviceLines.Count -eq 1) {
            $taskSerial = ($taskDeviceLines[0] -split '\s+')[0]
        } elseif ($taskDeviceLines.Count -gt 1) {
            throw 'Several Android devices are connected. Pass -Device SERIAL; use -Task Devices to list them.'
        } else {
            & $PSCommandPath -Task Emulator -Headless:$Headless
            $taskDeadline = (Get-Date).AddMinutes(3)
            do {
                Start-Sleep -Seconds 2
                $taskDeviceLines = @(& $taskAdb devices | Where-Object { $_ -match '^([^\s]+)\s+device$' })
            } until ($taskDeviceLines.Count -gt 0 -or (Get-Date) -gt $taskDeadline)
            if ($taskDeviceLines.Count -eq 0) { throw 'No device booted. See tmp/emulator-error.log.' }
            $taskSerial = ($taskDeviceLines[0] -split '\s+')[0]
        }
        $taskDeadline = (Get-Date).AddMinutes(3)
        do {
            $taskBoot = & $taskAdb -s $taskSerial shell getprop sys.boot_completed 2>$null
            if ($taskBoot -eq '1') { break }
            Start-Sleep -Seconds 2
        } until ((Get-Date) -gt $taskDeadline)
        if ($taskBoot -ne '1') { throw 'Android has not finished booting.' }
        & $taskAdb -s $taskSerial install -r app/build/outputs/apk/debug/app-debug.apk
        if ($LASTEXITCODE -ne 0) { throw 'APK installation failed.' }
        & $taskAdb -s $taskSerial shell am start -W -n com.screenhelper/.presentation.MainActivity
    }
    'Instrument' { Invoke-DeviceTests }
    'Screenshots' {
        New-Item -ItemType Directory -Path tmp/screenshots -Force | Out-Null
        New-Item -ItemType Directory -Path tmp/metrics -Force | Out-Null
        & $taskAdb pull /sdcard/Android/data/com.screenhelper/files/test-screenshots/. tmp/screenshots/
        if ($LASTEXITCODE -ne 0) { throw 'Test screenshots were not found; run Instrument first.' }
        & $taskAdb pull /sdcard/Android/data/com.screenhelper/files/test-metrics/. tmp/metrics/
    }
    'PageScreenshots' {
        New-Item -ItemType Directory -Path tmp/screenshots -Force | Out-Null
        foreach ($taskPage in @('home','chat','memo','character','settings','relationship')) {
            & $taskAdb shell am start -W -n com.screenhelper/.presentation.MainActivity --es tab $taskPage | Out-Null
            Start-Sleep -Seconds 1
            & $taskAdb shell screencap -p /sdcard/screenhelper-page.png
            & $taskAdb pull /sdcard/screenhelper-page.png ("tmp/screenshots/" + $taskPage + ".png")
        }
        & $taskAdb shell am start -n com.screenhelper/.presentation.MainActivity --es tab home | Out-Null
    }
    'Diagnostics' {
        New-Item -ItemType Directory -Path tmp/metrics -Force | Out-Null
        & $taskAdb install -r app/build/outputs/apk/debug/app-debug.apk
        if ($LASTEXITCODE -ne 0) { throw 'Install failed; build the app first.' }
        & $taskAdb shell getprop ro.build.version.release | Set-Content tmp/metrics/android-version.txt -Encoding UTF8
        & $taskAdb shell getprop ro.product.model | Set-Content tmp/metrics/device.txt -Encoding UTF8
        & $taskAdb shell am force-stop com.screenhelper
        & $taskAdb shell am start -W -n com.screenhelper/.presentation.MainActivity | Set-Content tmp/metrics/startup.txt -Encoding UTF8
        if ($LASTEXITCODE -ne 0) { throw 'The main activity did not start.' }
        Start-Sleep -Seconds 5
        & $taskAdb shell dumpsys meminfo com.screenhelper | Set-Content tmp/metrics/foreground-memory.txt -Encoding UTF8
        & $taskAdb shell dumpsys gfxinfo com.screenhelper | Set-Content tmp/metrics/foreground-frames.txt -Encoding UTF8
        & $taskAdb shell top -b -n 1 -p $(& $taskAdb shell pidof com.screenhelper) | Set-Content tmp/metrics/foreground-cpu.txt -Encoding UTF8
        & $taskAdb shell appops set com.screenhelper SYSTEM_ALERT_WINDOW allow
        & $taskAdb shell run-as com.screenhelper am start-foreground-service --user 0 -n com.screenhelper/.presentation.pet.PetService
        if ($LASTEXITCODE -ne 0) { throw 'Could not start the private overlay service for diagnostics.' }
        & $taskAdb shell input keyevent KEYCODE_HOME
        Start-Sleep -Seconds 10
        & $taskAdb shell dumpsys meminfo com.screenhelper | Set-Content tmp/metrics/overlay-memory.txt -Encoding UTF8
        & $taskAdb shell top -b -n 1 -p $(& $taskAdb shell pidof com.screenhelper) | Set-Content tmp/metrics/overlay-cpu.txt -Encoding UTF8
        & $taskAdb shell run-as com.screenhelper am stopservice --user 0 -n com.screenhelper/.presentation.pet.PetService
        & $taskAdb shell am start -W -n com.screenhelper/.presentation.MainActivity
        Get-Item app/build/outputs/apk/debug/app-debug.apk | Select-Object Name,Length | ConvertTo-Json | Set-Content tmp/metrics/apk-size.json -Encoding UTF8
    }
    'Emulator' {
        if (-not (Test-Path -LiteralPath (Join-Path $env:ANDROID_AVD_HOME 'ScreenHelper.avd'))) {
            'no' | & $taskAvdManager create avd --force --name ScreenHelper --package 'system-images;android-35;default;x86_64' --device 'pixel_5'
            if ($LASTEXITCODE -ne 0) { throw 'AVD creation failed.' }
        }
        $taskEmulator = Join-Path $env:ANDROID_HOME 'emulator/emulator.exe'
        $taskEmulatorArgs = @('-avd','ScreenHelper','-no-audio','-no-boot-anim','-gpu','swiftshader_indirect','-memory','2048','-cores','4','-no-snapshot')
        if ($Headless) { $taskEmulatorArgs += '-no-window' }
        Start-Process -FilePath $taskEmulator -ArgumentList $taskEmulatorArgs -RedirectStandardOutput (Join-Path $taskProjectRoot 'tmp/emulator.log') -RedirectStandardError (Join-Path $taskProjectRoot 'tmp/emulator-error.log') -WindowStyle Hidden
    }
    'StopEmulator' { & adb emu kill }
}
if ($Task -notin @('Emulator','StopEmulator') -and $LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

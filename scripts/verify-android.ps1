param(
    [Parameter(Mandatory = $true)][string]$Serial,
    [string]$Sdk = $env:ANDROID_HOME
)
$ErrorActionPreference = 'Stop'
if (-not $Sdk) { $Sdk = $env:ANDROID_SDK_ROOT }
if (-not $Sdk) { throw 'Set ANDROID_HOME or pass -Sdk with an Android SDK path.' }
$adb = Join-Path $Sdk 'platform-tools/adb.exe'
if (-not (Test-Path -LiteralPath $adb)) { throw 'Set ANDROID_HOME or pass -Sdk with an Android SDK path.' }

function Invoke-Adb {
    param([string[]]$Arguments)
    $result = & $adb -s $Serial @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw ($result -join "`n") }
    return ($result -join "`n").Trim()
}

$root = Split-Path $PSScriptRoot -Parent
Push-Location $root
try {
    $devices = & $adb devices
    if ($LASTEXITCODE -ne 0 -or ($devices -join "`n") -notmatch "(?m)^$([regex]::Escape($Serial))\s+device\b") {
        throw 'Abre el emulador y revisa su nombre con adb devices -l.'
    }
    if ((Invoke-Adb -Arguments @('get-state')) -ne 'device') {
        throw 'Selected device is not ready. Check adb devices -l.'
    }
    $deadline = (Get-Date).AddSeconds(120)
    while ((Invoke-Adb -Arguments @('shell', 'getprop', 'sys.boot_completed')) -ne '1') {
        if ((Get-Date) -gt $deadline) { throw 'Emulator boot timed out after 120 seconds.' }
        Start-Sleep -Seconds 2
    }
    $api = [int](Invoke-Adb -Arguments @('shell', 'getprop', 'ro.build.version.sdk'))
    if ($api -lt 24) { throw "Device API $api is below minSdk 24." }

    & .\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed; installation was not attempted.' }
    $apk = Join-Path $root 'app/build/outputs/apk/debug/app-debug.apk'
    Write-Output (Invoke-Adb -Arguments @('install', '-r', $apk))
    Write-Output (Invoke-Adb -Arguments @('shell', 'am', 'start', '-W', '-S', '-n', 'com.example.thevault/.MainActivity'))
    Start-Sleep -Seconds 5
    $appProcess = Invoke-Adb -Arguments @('shell', 'pidof', 'com.example.thevault')
    if (-not $appProcess) { throw 'Application exited after launch.' }
    Write-Output "TheVault is running on $Serial (API $api), process $appProcess."
    $testApk = Join-Path $root 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
    Write-Output (Invoke-Adb -Arguments @('install', '-r', $testApk))
    $testResult = Invoke-Adb -Arguments @('shell', 'am', 'instrument', '-w', 'com.example.thevault.test/androidx.test.runner.AndroidJUnitRunner')
    Write-Output $testResult
    if ($testResult -notmatch 'OK \([1-9][0-9]* tests?\)' -or
        $testResult -match 'FAILURES|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED') {
        throw 'Instrumented tests failed.'
    }
} catch {
    & $adb -s $Serial logcat -d -t 200 -s AndroidRuntime:E
    throw
} finally {
    Pop-Location
}

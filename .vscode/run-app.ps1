param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("emulator", "salatsign", "salatschedule", "test", "logcat")]
    [string]$Action
)

$ErrorActionPreference = "Stop"

$sdk = if ($env:ANDROID_HOME -and (Test-Path $env:ANDROID_HOME)) {
    $env:ANDROID_HOME
} else {
    Join-Path $env:LOCALAPPDATA "Android\Sdk"
}
$env:ANDROID_HOME = $sdk

$adb = Join-Path $sdk "platform-tools\adb.exe"
$emu = Join-Path $sdk "emulator\emulator.exe"
$avdName = "PhotoDream_TV_API_26"

function Ensure-Device {
    $devices = (& $adb devices) | Select-String "\tdevice$"
    if ($devices) {
        Write-Host "Using connected Android device/emulator: $devices"
        return
    }
    Write-Host "No device attached. Starting Android TV emulator ($avdName)..."
    Start-Process -FilePath $emu -ArgumentList "-avd", $avdName
    & $adb wait-for-device
    Write-Host "Waiting for Android TV boot to complete..."
    while (((& $adb shell getprop sys.boot_completed) -join "").Trim() -ne "1") {
        Start-Sleep -Seconds 2
    }
    Write-Host "Emulator is ready."
}

switch ($Action) {
    "emulator" {
        Ensure-Device
    }
    "salatsign" {
        Ensure-Device
        & .\gradlew.bat :app-salatsign:installDebug
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        & $adb shell am start -n com.salatsign.tv/.MainActivity
    }
    "salatschedule" {
        Ensure-Device
        & .\gradlew.bat :app-salatschedule:installDebug
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        & $adb shell am start -n com.salatschedule.tv/.MainActivity
    }
    "test" {
        & .\gradlew.bat testDebugUnitTest lintDebug
        exit $LASTEXITCODE
    }
    "logcat" {
        & $adb logcat -s SalatSignLocalServer SalatSignNsdHelper PrayerAlarmScheduler PrayerAlarmReceiver AdhanPlaybackService DailyScheduleWorker AndroidRuntime
    }
}

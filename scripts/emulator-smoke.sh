#!/usr/bin/env bash
set -euo pipefail
mkdir -p smoke-output
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w io.wayfarer.rpg.action.test/io.wayfarer.rpg.SmokeInstrumentation | tee smoke-output/instrumentation.txt
adb pull /sdcard/Android/data/io.wayfarer.rpg.action/files/smoke smoke-output/screenshots || true
adb logcat -d -s AndroidRuntime:E > smoke-output/crashes.txt
grep -q ARPG_SMOKE_OK smoke-output/instrumentation.txt

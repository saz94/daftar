#!/usr/bin/env bash
# Runs inside the CI emulator: installs the app with sample data and photographs its screens.
set -u
APK=app/build/outputs/apk/debug/app-debug.apk
OUT=shots
mkdir -p "$OUT"
adb install -r "$APK"
adb shell settings put system screen_off_timeout 1800000 || true

shot() { # route name
  adb shell am start -S -W -n com.daftar.app/.MainActivity --ez demo true --es route "$1" >/dev/null
  sleep "${3:-5}"
  adb exec-out screencap -p > "$OUT/$2.png"
  echo "captured $2"
}

adb shell cmd uimode night no
shot home warmup 12
shot home en_home
adb shell input swipe 540 1900 540 500 400; sleep 2
adb exec-out screencap -p > "$OUT/en_home_2.png"
shot create en_create
shot docs en_invoices
shot preview/11 en_preview 8
shot doc/INVOICE/11 en_editor
shot money en_money
shot more en_more
shot business/1 en_business
shot loans en_loans
shot shareholders en_shareholders
shot tax en_tax

adb shell cmd uimode night yes
shot home dark_home
shot docs dark_invoices
shot preview/11 dark_preview 8
adb shell cmd uimode night no

adb shell cmd locale set-app-locales com.daftar.app --locales ar || true
sleep 2
shot home ar_home 7
shot docs ar_invoices
shot preview/12 ar_preview 8
shot create ar_create
adb shell cmd uimode night yes
shot home ar_dark_home
ls -la "$OUT"

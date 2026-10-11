#!/usr/bin/env bash
# Runs inside the CI emulator: installs the app with sample data and photographs its screens.
set -u
APK=app/build/outputs/apk/debug/app-debug.apk
OUT=shots
mkdir -p "$OUT"
adb install -r "$APK"
adb shell settings put system screen_off_timeout 1800000 || true
# the CI emulator is slow enough that the launcher sometimes trips "isn't responding"; keep that off the pictures
adb shell settings put global hide_error_dialogs 1 || true
adb shell settings put secure anr_show_background 0 || true
adb shell input keyevent KEYCODE_HOME; sleep 20

shot() { # route name
  adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null 2>&1 || true
  adb shell am start -S -W -n com.daftar.app/.MainActivity --ez demo true --es route "$1" >/dev/null
  sleep "${3:-10}"
  adb exec-out screencap -p > "$OUT/$2.png"
  echo "captured $2"
}

adb shell cmd uimode night no
shot home warmup 25
shot home en_home
adb shell input swipe 540 1900 540 500 400; sleep 2
adb exec-out screencap -p > "$OUT/en_home_2.png"
shot create en_create
shot docs en_invoices
shot preview/11 en_preview 12
shot doc/INVOICE/11 en_editor
shot money en_money
shot more en_more
shot business/1 en_business
shot loans en_loans
shot shareholders en_shareholders
shot tax en_tax
shot settings en_settings
shot lock en_lock
shot setpin en_setpin

# widgets: ask the launcher to place each one, press its "Add" button, then photograph the home screen
pin() { # route name
  adb shell am start -S -W -n com.daftar.app/.MainActivity --ez demo true --es route "$1" >/dev/null
  sleep 10
  adb exec-out screencap -p > "$OUT/$2_dialog.png"
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb exec-out cat /sdcard/ui.xml > /tmp/ui.xml || true
  XY=$(python3 - <<'PY'
import re
x = open('/tmp/ui.xml', encoding='utf-8', errors='ignore').read()
best = None
for m in re.finditer(r'text="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', x):
    t = m.group(1).strip().lower()
    if t in ('add to home screen', 'add automatically', 'add'):
        a, b, c, d = map(int, m.groups()[1:])
        best = ((a + c) // 2, (b + d) // 2)
print('%d %d' % best if best else '')
PY
)
  echo "pin button at: $XY"
  if [ -n "$XY" ]; then adb shell input tap $XY; sleep 4; fi
}
pin pinwidget widget_snapshot
pin pinactions widget_actions
adb shell input keyevent KEYCODE_HOME; sleep 6
adb exec-out screencap -p > "$OUT/widgets_home.png"
adb shell input swipe 800 1200 200 1200 300; sleep 3
adb exec-out screencap -p > "$OUT/widgets_home_2.png"
shot products en_products
shot doc/QUOTATION/0 en_new_quote

adb shell cmd uimode night yes
shot home dark_home
shot docs dark_invoices
shot preview/11 dark_preview 12
adb shell cmd uimode night no

adb shell cmd locale set-app-locales com.daftar.app --locales ar || true
sleep 2
shot home ar_home 12
shot docs ar_invoices
shot preview/12 ar_preview 12
shot create ar_create
adb shell cmd uimode night yes
shot home ar_dark_home
ls -la "$OUT"

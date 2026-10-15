#!/usr/bin/env bash
# ci/screens_adaptive.sh TAG FOLDABLE(0|1) [TABLETOP_ROT]
# TABLETOP_ROT is the rotation in which a half-open device has its hinge across the middle:
# 1 (landscape) for a book-style foldable, 0 (portrait) for a flip-style one.
# Photographs the cover and inner experiences on a foldable or tablet emulator, and checks that
# folding keeps the app alive (same process) with its state (text typed into the search box).
set -u
TAG=$1; FOLDABLE=$2; TABLETOP_ROT=${3:-1}
if [ "$TABLETOP_ROT" = 1 ]; then BOOK_ROT=0; else BOOK_ROT=1; fi
APK=app/build/outputs/apk/debug/app-debug.apk
OUT=shots-$TAG
mkdir -p "$OUT"
adb install -r "$APK"
adb shell settings put system screen_off_timeout 1800000 || true
adb shell settings put global hide_error_dialogs 1 || true
adb shell settings put system accelerometer_rotation 0 || true
adb shell input keyevent KEYCODE_HOME; sleep 15

shot() { # route name [wait]
  adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null 2>&1 || true
  adb shell am start -S -W -n com.daftar.app/.MainActivity --ez demo true --es route "$1" >/dev/null
  sleep "${3:-10}"
  adb exec-out screencap -p > "$OUT/$2.png"
  echo "captured $2"
}
snap() { sleep "${2:-3}"; adb exec-out screencap -p > "$OUT/$1.png"; echo "captured $1"; }
rotate() { adb shell settings put system user_rotation "$1"; adb shell wm user-rotation lock "$1" >/dev/null 2>&1 || true; sleep 5; }
screen_text() { adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb exec-out cat /sdcard/ui.xml > /tmp/ui.xml || true; }
tap_text() { # taps the centre of the element whose text contains this
  screen_text
  XY=$(T="$1" python3 - <<'PY'
import os, re
x = open('/tmp/ui.xml', encoding='utf-8', errors='ignore').read()
for m in re.finditer(r'text="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', x):
    if os.environ['T'] in m.group(1):
        a, b, c, d = map(int, m.groups()[1:])
        print((a + c) // 2, (b + d) // 2)
        break
PY
)
  if [ -n "$XY" ]; then adb shell input tap $XY; sleep 2; else echo "could not find '$1'"; fi
}
has() { screen_text; if grep -q "$1" /tmp/ui.xml; then echo "PASS: $2"; else echo "FAIL: $2"; fi; }

shot home warmup 25

if [ "$FOLDABLE" = 1 ]; then
  # ---- cover experience (folded)
  adb emu fold; sleep 6
  adb shell wm size > "$OUT/wm_size_folded.txt"; cat "$OUT/wm_size_folded.txt"
  adb shell dumpsys window | grep -i -E "folding|hinge|posture" | head -5 >> "$OUT/wm_size_folded.txt" || true
  shot home cover_home
  shot docs cover_docs
  shot money cover_money
  shot create cover_create

  # ---- inner experience (unfolded, flat)
  adb emu unfold; sleep 6
  shot home inner_home
  shot docs inner_docs_empty
  tap_text "Desert Rose"; snap inner_docs 8
  shot money inner_money_empty
  tap_text "DEWA"; snap inner_money
  shot more inner_more_empty
  tap_text "Products"; snap inner_more
  shot lock inner_lock
  shot doc/INVOICE/11 inner_editor
  adb shell cmd uimode night yes; shot home inner_home_dark; adb shell cmd uimode night no
  adb shell cmd locale set-app-locales com.daftar.app --locales ar || true; sleep 2
  shot home inner_home_ar 12
  shot docs inner_docs_ar_empty; tap_text "Desert Rose"; snap inner_docs_ar 8
  adb shell cmd locale set-app-locales com.daftar.app --locales en || true; sleep 2

  # ---- half open like a book (hinge top to bottom): panes split on the hinge
  adb emu posture 2; sleep 6
  rotate $BOOK_ROT
  shot docs book_docs_empty; tap_text "Desert Rose"; snap book_docs 8
  shot home book_home

  # ---- tabletop: half open with the hinge across the middle
  rotate $TABLETOP_ROT
  shot home tabletop_home 12
  shot lock tabletop_lock
  shot docs tabletop_docs
  rotate 0
  adb emu posture 3; sleep 4

  # ---- fold and unfold with the app open: same process, typed text still there
  {
    shot docs state_before
    tap_text "Search by name"; adb shell input text "Gulf"; sleep 2
    PID1=$(adb shell pidof com.daftar.app | tr -d '\r')
    adb emu fold; sleep 6; snap state_folded 2
    has "Gulf" "search text survives folding"
    adb emu unfold; sleep 6; snap state_unfolded 2
    PID2=$(adb shell pidof com.daftar.app | tr -d '\r')
    has "Gulf" "search text survives unfolding"
    if [ -n "$PID1" ] && [ "$PID1" = "$PID2" ]; then echo "PASS: same process before and after folding ($PID1)"; else echo "FAIL: process changed ($PID1 -> $PID2)"; fi
    rotate 1; sleep 3; PID3=$(adb shell pidof com.daftar.app | tr -d '\r'); has "Gulf" "search text survives rotation"
    if [ "$PID1" = "$PID3" ]; then echo "PASS: same process after rotation"; else echo "FAIL: process changed on rotation"; fi
    rotate 0
  } > "$OUT/fold_test.txt" 2>&1
  cat "$OUT/fold_test.txt"
else
  # ---- tablets: portrait and landscape
  shot home portrait_home
  shot docs portrait_docs_empty; tap_text "Desert Rose"; snap portrait_docs 8
  shot money portrait_money_empty; tap_text "DEWA"; snap portrait_money
  rotate 1
  shot home landscape_home 12
  shot docs landscape_docs_empty; tap_text "Desert Rose"; snap landscape_docs 8
  shot more landscape_more_empty; tap_text "Loans"; snap landscape_more
  shot lock landscape_lock
  adb shell cmd locale set-app-locales com.daftar.app --locales ar || true; sleep 2
  shot home landscape_home_ar 12
  adb shell cmd locale set-app-locales com.daftar.app --locales en || true
  rotate 0
fi
ls -la "$OUT"

#!/usr/bin/env bash
# ci/avd_hinge.sh PROFILE
# avdmanager creates foldable AVDs without their hinge, so "adb emu fold" does nothing. This adds
# the hinge the same way Android Studio's device manager does, before the emulator starts.
set -u
PROFILE=$1
CFG=$(ls -d "$HOME"/.android/avd/*.avd 2>/dev/null | head -1)/config.ini
[ -f "$CFG" ] || { echo "no AVD config found"; exit 0; }
case "$PROFILE" in
  "7.6in Foldable")
    # 1768 x 2208 inner panel, hinge top to bottom; folded, the left half (884 wide) is the cover
    cat >> "$CFG" <<'INI'
hw.sensor.hinge = yes
hw.sensor.hinge.count = 1
hw.sensor.hinge.type = 1
hw.sensor.hinge.sub_type = 1
hw.sensor.hinge.ranges = 0-180
hw.sensor.hinge.defaults = 180
hw.sensor.hinge.areas = 884-0-1-2208
hw.sensor.posture_list = 1, 2, 3
hw.sensor.hinge_angles_posture_definitions = 0-30, 30-150, 150-180
hw.sensor.hinge.fold_to_displayRegion.0.1_at_posture = 1
hw.displayRegion.0.1.xOffset = 0
hw.displayRegion.0.1.yOffset = 0
hw.displayRegion.0.1.width = 884
hw.displayRegion.0.1.height = 2208
INI
    ;;
  "6.7in Foldable")
    # 1080 x 2636 panel, hinge across the middle (flip); folded, the top half is the cover
    cat >> "$CFG" <<'INI'
hw.sensor.hinge = yes
hw.sensor.hinge.count = 1
hw.sensor.hinge.type = 0
hw.sensor.hinge.sub_type = 1
hw.sensor.hinge.ranges = 0-180
hw.sensor.hinge.defaults = 180
hw.sensor.hinge.areas = 0-1318-1080-1
hw.sensor.posture_list = 1, 2, 3
hw.sensor.hinge_angles_posture_definitions = 0-30, 30-150, 150-180
hw.sensor.hinge.fold_to_displayRegion.0.1_at_posture = 1
hw.displayRegion.0.1.xOffset = 0
hw.displayRegion.0.1.yOffset = 0
hw.displayRegion.0.1.width = 1080
hw.displayRegion.0.1.height = 1318
INI
    ;;
esac
echo "--- $CFG"; grep -E "hinge|displayRegion|hw.lcd" "$CFG"

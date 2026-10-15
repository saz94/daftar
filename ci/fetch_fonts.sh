#!/usr/bin/env bash
# Downloads the app's typeface (IBM Plex Sans Arabic, SIL Open Font License) into the project.
# Run once before the first local build:  bash ci/fetch_fonts.sh
set -euo pipefail
dir="$(cd "$(dirname "$0")/.." && pwd)/app/src/main/res/font"
mkdir -p "$dir"
base="https://raw.githubusercontent.com/google/fonts/main/ofl/ibmplexsansarabic"
for pair in Light:plex_light Regular:plex_regular Medium:plex_medium SemiBold:plex_semibold Bold:plex_bold; do
  src="${pair%%:*}"; dst="${pair##*:}"
  if [ ! -s "$dir/$dst.ttf" ]; then
    curl -fsSL --retry 3 "$base/IBMPlexSansArabic-$src.ttf" -o "$dir/$dst.ttf"
  fi
  # a real TrueType file starts with 00 01 00 00
  head -c 4 "$dir/$dst.ttf" | od -An -tx1 | grep -q "00 01 00 00" || { echo "Bad font file: $dst"; exit 1; }
done
ls -la "$dir"

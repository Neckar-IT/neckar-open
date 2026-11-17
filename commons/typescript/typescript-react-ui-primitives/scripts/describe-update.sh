#!/usr/bin/env bash
#
# describe-update.sh - Prints the merge request description for a commit of update-primitives.sh.
#
# Lists every file the commit changed, pnpm-lock.yaml included, and the diff of the theme file
# that components.json names in `tailwind.css` when the commit changed it.
#
# Usage:
#   describe-update.sh [<commit>]   (default: HEAD)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PACKAGE_DIR="$(dirname "$SCRIPT_DIR")"
COMMIT="${1:-HEAD}"

package_path="$(git -C "$PACKAGE_DIR" rev-parse --show-prefix)"
theme_file="$package_path$(jq -r '(.tailwind.css | select(. != "")) // error("components.json names no tailwind.css")' "$PACKAGE_DIR/components.json")"

echo "The shadcn CLI rewrote every installed primitive with the current registry state. A changed component can look different, so the merge request needs a review before the merge."
echo
echo "## Changed files"
echo
echo "| Status | File |"
echo "|---|---|"
git -C "$PACKAGE_DIR" diff --name-status --no-renames "$COMMIT~1" "$COMMIT" |
  while IFS=$'\t' read -r status path; do
    echo "| $status | \`$path\` |"
  done

theme_diff="$(git -C "$PACKAGE_DIR" diff "$COMMIT~1" "$COMMIT" -- ":/$theme_file")"
if [[ -n "$theme_diff" ]]; then
  echo
  echo "## \`$theme_file\`"
  echo
  echo '```diff'
  echo "$theme_diff"
  echo '```'
fi

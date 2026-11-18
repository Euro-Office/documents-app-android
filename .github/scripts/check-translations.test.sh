#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Euro-Office contributors
# SPDX-License-Identifier: AGPL-3.0-only

# Self-check of check-translations.sh: each line is the expected exit code and a changed path

cd "$(dirname "$0")" || exit 1
failed=0
check() {
  local want=$1 got
  shift
  printf "%s\n" "$@" | ./check-translations.sh >/dev/null
  got=$?
  if [[ $got != "$want" ]]; then
    echo "FAIL: expected exit $want, got $got for: $*"
    failed=1
  fi
}

while read -r want file; do
  check "$want" "$file"
done <<EOF
1 app_manager/appmanager/src/main/res/values-de/translatable.xml
1 app_manager/appmanager/src/main/res/values-pt-rBR/translatable.xml
0 app_manager/appmanager/src/main/res/values/translatable.xml
0 toolkit/libtoolkit/src/main/res/values-de/translatable.xml
0 toolkit/libtoolkit/src/main/res/values-de/dimens.xml
0 README.md
EOF

# a violation among allowed files fails, no changes pass
check 1 app_manager/appmanager/src/main/res/values/translatable.xml app_manager/appmanager/src/main/res/values-de/translatable.xml
check 0 ''

(( failed )) || echo ok
exit $failed

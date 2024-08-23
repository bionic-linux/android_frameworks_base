#!/usr/bin/env bash

# Copyright (C) 2024 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -e

ROOT_DIR="."
if [ "$ANDROID_BUILD_TOP" != "" ]; then
  ROOT_DIR="$ANDROID_BUILD_TOP"
fi

SRCS_DIR="tests/srcs"
GOLDEN_DIR="tests/golden"
REGEN_SRCS=0
# We're running via command-line, so ensure we have fresh srcs and use absolute paths.
if [[ $(basename $0) == "golden_test.sh" ]]; then
  REGEN_SRCS=1
  SRCS_DIR="$ROOT_DIR/out/soong/.intermediates/frameworks/base/tools/systemfeatures/systemfeatures-gen-tests-srcs/gen/tests/srcs"
  GOLDEN_DIR="$ROOT_DIR/frameworks/base/tools/systemfeatures/tests/golden"
fi

UPDATE_GOLDEN=0
if [[ "$1" == "--update" ]]; then
  REGEN_SRCS=1
  UPDATE_GOLDEN=1
fi

if [ "$REGEN_SRCS" = 1 ]; then
  if [ -z $ANDROID_BUILD_TOP ]; then
    echo "You need to source and lunch before you can use this script"
    exit 1
  fi
  rm -rf "$SRCS_DIR"
  "$ROOT_DIR"/build/soong/soong_ui.bash --make-mode systemfeatures-gen-tests-srcs
fi

if [ "$UPDATE_GOLDEN" = 1 ]; then
  rm -rf "$GOLDEN_DIR"
  cp -R "$SRCS_DIR" "$GOLDEN_DIR"
  echo "Updated golden test files."
else
  echo "Running diff from test output against golden test files..."
  if diff -ruN "$GOLDEN_DIR" "$SRCS_DIR" ; then
    echo "No changes."
  else
    echo
    echo "----------------------------------------------------------------------------------------"
    echo "If changes look OK, run:"
    echo "  \$ANDROID_BUILD_TOP/frameworks/base/tools/systemfeatures/golden_test.sh --update"
    echo "----------------------------------------------------------------------------------------"
    exit 1
  fi
fi

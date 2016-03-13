#!/usr/bin/env bash

# Copyright 2016 The Android Open Source Project
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

# This script regenerates AllAudio.mk based on the content of the other
# makefiles.

# It needs to be run from its location in the source tree.

# Start fresh by removing the existing AllAudio.mk file
rm AllAudio.mk

cat > AllAudio.mk << EOF
# Copyright 2016 The Android Open Source Project
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

LOCAL_PATH := frameworks/base/data/sounds

PRODUCT_COPY_FILES += \\
EOF

# Starting from https://android.googlesource.com/platform/frameworks/base/+/dcbdd3b420254ce39d9793c6fde6dd05436704ff ,
# AudioPackage12.mk+ packages will break the AllAudio.mk after running this script.
# To solve this problem, the script will only generate AllAudio.mk from AudioPackage{2..11}.mk
# and OriginalAudio.mk files
#
# TODO add a method to include audio from new makefiles into AllAudio.mk

cat OriginalAudio.mk AudioPackage{2..11}.mk |
  grep \\\$\(LOCAL_PATH\).*: |
  cut -d : -f 2 |
  cut -d \  -f 1 |
  sort -u |
  while read DEST
  do
    echo -n \ \ \ \  >> AllAudio.tmp
    cat *.mk |
      grep \\\$\(LOCAL_PATH\).*:$DEST |
      tr -d \ \\t |
      cut -d : -f 1 |
      sort -u |
      tail -n 1 |
      tr -d \\n >> AllAudio.tmp
    echo :$DEST\ \\ >> AllAudio.tmp
  done
# Sort paths alphabetically to AllAudio.mk and remove AllAudio.tmp
  sort -d AllAudio.tmp >> AllAudio.mk
  rm AllAudio.tmp

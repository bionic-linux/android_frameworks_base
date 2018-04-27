/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <sstream>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "androidfw/ApkAssets.h"
#include "androidfw/Idmap.h"

#include "idmap2/Idmap.h"
#include "idmap2/PrettyPrintVisitor.h"

#include "TestHelpers.h"

using ::testing::IsNull;
using ::testing::NotNull;

using android::ApkAssets;

namespace android {
namespace idmap2 {

TEST(PrettyPrintVisitorTests, CreatePrettyPrintVisitor) {
  const std::string target_apk_path(GetTestDataPath() + "/target/target.apk");
  std::unique_ptr<const ApkAssets> target_apk = ApkAssets::Load(target_apk_path);
  ASSERT_THAT(target_apk, NotNull());

  const std::string overlay_apk_path(GetTestDataPath() + "/overlay/overlay.apk");
  std::unique_ptr<const ApkAssets> overlay_apk = ApkAssets::Load(overlay_apk_path);
  ASSERT_THAT(overlay_apk, NotNull());

  std::stringstream error;
  std::unique_ptr<const Idmap> idmap =
      Idmap::FromApkAssets(target_apk_path, *target_apk, overlay_apk_path, *overlay_apk, error);
  ASSERT_THAT(idmap, NotNull());

  std::stringstream stream;
  PrettyPrintVisitor visitor(stream);
  idmap->accept(visitor);

  // FIXME: print to stdout for now for manual debugging, switch to prodding
  // with regex at a later point in time
  printf("%s", stream.str().c_str());
}

// FIXME: add test where idmap is created FromBinaryStream: this will lead to
// target apk not possible to open, which should limit the output (i.e. keep
// "resid -> resid" but don't print "type/name")

}  // namespace idmap2
}  // namespace android

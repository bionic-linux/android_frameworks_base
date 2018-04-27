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

#include <cstdio>  // fclose

#include "idmap2/ZipFile.h"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "TestHelpers.h"

using ::testing::IsNull;
using ::testing::NotNull;

namespace android {
namespace idmap2 {

TEST(ZipFileTests, BasicOpen) {
  auto zip = ZipFile::Open(GetTestDataPath() + "/target/target.apk");
  ASSERT_THAT(zip, NotNull());

  fclose(stderr);  // silence expected warnings from libziparchive
  auto fail = ZipFile::Open(GetTestDataPath() + "/does-not-exist");
  ASSERT_THAT(fail, IsNull());
}

TEST(ZipFileTests, Crc) {
  auto zip = ZipFile::Open(GetTestDataPath() + "/target/target.apk");
  ASSERT_THAT(zip, NotNull());

  uint32_t crc;
  bool status = zip->Crc("AndroidManifest.xml", crc);
  ASSERT_TRUE(status);
  ASSERT_EQ(crc, 0xc107e458);

  status = zip->Crc("does-not-exist", crc);
  ASSERT_FALSE(status);
}

TEST(ZipFileTests, Uncompress) {
  auto zip = ZipFile::Open(GetTestDataPath() + "/target/target.apk");
  ASSERT_THAT(zip, NotNull());

  auto data = zip->Uncompress("assets/lorem-ipsum.txt");
  ASSERT_THAT(data, NotNull());
  const std::string lorem_ipsum("Lorem ipsum dolor sit amet.\n");
  ASSERT_THAT(data->size, lorem_ipsum.size());
  ASSERT_THAT(std::string(reinterpret_cast<const char*>(data->buf), data->size), lorem_ipsum);

  auto fail = zip->Uncompress("does-not-exist");
  ASSERT_THAT(fail, IsNull());
}

}  // namespace idmap2
}  // namespace android

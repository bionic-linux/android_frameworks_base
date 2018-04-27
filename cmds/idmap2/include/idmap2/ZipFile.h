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

#ifndef IDMAP2_ZIPFILE_H
#define IDMAP2_ZIPFILE_H

#include <memory>
#include <string>

#include "android-base/macros.h"
#include "ziparchive/zip_archive.h"

namespace android {
namespace idmap2 {

struct MemoryChunk {
  size_t size;
  uint8_t buf[0];
};

class ZipFile {
 public:
  static std::unique_ptr<const ZipFile> Open(const std::string& path);

  std::unique_ptr<const MemoryChunk> Uncompress(const std::string& entryPath) const;
  bool Crc(const std::string& entryPath, uint32_t& out) const;

  ~ZipFile();

 private:
  DISALLOW_COPY_AND_ASSIGN(ZipFile);
  ZipFile(const ::ZipArchiveHandle handle) : handle_(handle) {
  }

  const ::ZipArchiveHandle handle_;
};

}  // namespace idmap2
}  // namespace android

#endif

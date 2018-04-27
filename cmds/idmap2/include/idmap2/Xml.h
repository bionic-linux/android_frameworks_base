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

#ifndef IDMAP2_XML_H
#define IDMAP2_XML_H

#include <map>
#include <memory>
#include <string>

#include "android-base/macros.h"
#include "androidfw/ResourceTypes.h"
#include "utils/String16.h"

namespace android {
namespace idmap2 {

class Xml {
 public:
  static std::unique_ptr<const Xml> Create(const uint8_t* data, size_t size, bool copyData = false);

  std::unique_ptr<std::map<std::string, std::string>> FindTag(const std::string& name) const;

  ~Xml();

 private:
  DISALLOW_COPY_AND_ASSIGN(Xml);
  Xml() {
  }

  mutable ResXMLTree xml_;
};

}  // namespace idmap2
}  // namespace android

#endif

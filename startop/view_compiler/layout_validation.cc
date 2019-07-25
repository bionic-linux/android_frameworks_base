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

#include "layout_validation.h"

#include "android-base/logging.h"
#include "android-base/stringprintf.h"

namespace startop {

constexpr std::u16string_view SUPPORTED_ATTRIBUTES[] = {
  u"id",
  u"layout_width",
  u"layout_height",
};

void LayoutValidationVisitor::VisitStartTag(const std::u16string_view name, const AttributeSet& attrs) {
  LOG(INFO) << "Checking tag " << std::u16string{name}.c_str();
  if (0 == name.compare(u"merge")) {
    message_ = "Merge tags are not supported";
    can_compile_ = false;
  }
  if (0 == name.compare(u"include")) {
    message_ = "Include tags are not supported";
    can_compile_ = false;
  }
  if (0 == name.compare(u"view")) {
    message_ = "View tags are not supported";
    can_compile_ = false;
  }
  if (0 == name.compare(u"fragment")) {
    message_ = "Fragment tags are not supported";
    can_compile_ = false;
  }

  // If we still support the XML-free fast path, make sure none of the
  // attributes here invalidate it.
  if (can_compile_xml_free()) {
    for (const Attribute& attr : attrs) {
      LOG(INFO) << "  Checking attribute " << std::u16string{attr.name()}.c_str();
      bool is_supported = false;
      for(auto supported_attribute : SUPPORTED_ATTRIBUTES) {
        if (0 == attr.name().compare(supported_attribute)) {
          is_supported = true;
          break;
        }
      }
      if (!is_supported) {
        can_compile_xml_free_ = false;
        break;
      }
    }
  }
}

}  // namespace startop
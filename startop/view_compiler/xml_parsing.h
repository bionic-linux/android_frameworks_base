/*
 * Copyright (C) 2019 The Android Open Source Project
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

#ifndef XML_PARSING_H_
#define XML_PARSING_H_

#include <string>
#include <vector>

namespace startop {

// The viewcompiler currently supports reading from two XML-like sources. The
// first is a textual XML file, which is powered by TinyXML. The second is a
// binary resource file embedded within an APK. The APIs for these two sources
// are slightly different. This file defines some interfaces that are used to
// help smooth over the differences between the two parsers.

class Attribute {
 public:
  Attribute(const std::u16string_view name) : name_{name} {}

  const std::u16string_view name() const { return name_; }

 private:
  const std::u16string_view name_;
};

class AttributeSet {
 public:
  class Iterator {
   public:
    constexpr Iterator(const AttributeSet* parent, size_t index = 0) : parent(parent), i(index) {}
    const Attribute& operator*() const { return (*parent)[i]; }
    bool operator!=(const Iterator& rhs) const { return i != rhs.i; }
    Iterator& operator++() {
      i++;
      return *this;
    }

   private:
    const AttributeSet* parent;
    size_t i;
  };

  Iterator begin() const { return Iterator{this}; }
  Iterator end() const { return Iterator{this, attrs_.size()}; }

  const Attribute& operator[](size_t index) const { return attrs_[index]; }

  void push_back(Attribute&& attr) { attrs_.push_back(attr); }

 private:
  std::vector<Attribute> attrs_;
};

}  // namespace startop

#endif  // XML_PARSING_H_
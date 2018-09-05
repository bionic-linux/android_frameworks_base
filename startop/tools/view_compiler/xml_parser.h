/*
 * Copyright (C) 20185 The Android Open Source Project
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

#ifndef XML_PARSER_H_
#define XML_PARSER_H_

#include <expat.h>

#include <algorithm>
#include <istream>
#include <queue>
#include <string>

#include "android-base/macros.h"

// This is a simple wrapper around expat to facilitate parsing XML files. It is derived from aapt2's
// XML parser, but significantly cut down to only cover the use cases we need here.
//
// This parser is primarily meant for development and testing. Ultimately we should be extracting
// and compiling resources from APKs rather than source text.
class XmlTextParser {
 public:
  enum class Event { kBadDocument, kStartDocument, kEndDocument, kStartElement, kEndElement };

  explicit XmlTextParser(std::istream* in);
  ~XmlTextParser();

  /**
   * Returns the current event that is being processed.
   */
  Event event() const;
  const std::string& element_name() const;

  /**
   * Note, unlike ResXmlParser, the first call to next() will return
   * StartElement of the first element.
   * 
   * TODO: Next should have the same behavior as ResXmlParser
   */
  Event Next();

 private:
  DISALLOW_COPY_AND_ASSIGN(XmlTextParser);

  static void XMLCALL StartElementHandler(void* user_data, const char* name, const char** attrs);
  static void XMLCALL EndElementHandler(void* user_data, const char* name);

  struct EventData {
    Event event;
    std::string element_name{};
  };

  std::istream* in_;
  XML_Parser parser_;
  std::queue<EventData> event_queue_;
};

#endif  // XML_PARSER_H_

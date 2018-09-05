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

#include "xml_parser.h"

#include <iostream>
#include <string>

constexpr char kXmlNamespaceSep = 1;

XmlTextParser::XmlTextParser(std::istream* in) : in_(in) {
  parser_ = XML_ParserCreateNS(nullptr, kXmlNamespaceSep);
  XML_SetUserData(parser_, this);
  XML_SetElementHandler(parser_, StartElementHandler, EndElementHandler);
  event_queue_.push(EventData{Event::kStartDocument});
}

XmlTextParser::~XmlTextParser() { XML_ParserFree(parser_); }

XmlTextParser::Event XmlTextParser::Next() {
  const Event currentEvent = event();
  if (currentEvent == Event::kBadDocument || currentEvent == Event::kEndDocument) {
    return currentEvent;
  }

  constexpr size_t kBufferSize = 4096;
  std::unique_ptr<char[]> buffer{std::make_unique<char[]>(kBufferSize)};

  event_queue_.pop();
  while (event_queue_.empty()) {
    in_->read(buffer.get(), kBufferSize);
    size_t const bytes_read = in_->gcount();

    bool done = false;

    if (in_->bad()) {
      event_queue_.push(EventData{Event::kBadDocument});
      break;
    }
    if (in_->eof()) {
      done = true;
    }

    if (XML_Parse(parser_, buffer.get(), bytes_read, done) == XML_STATUS_ERROR) {
      event_queue_.push(EventData{Event::kBadDocument});
      break;
    }

    if (done) {
      event_queue_.push(EventData{Event::kEndDocument});
    }
  }

  Event next_event = event();

  return next_event;
}

XmlTextParser::Event XmlTextParser::event() const { return event_queue_.front().event; }

const std::string& XmlTextParser::element_name() const { return event_queue_.front().element_name; }

namespace {

/**
 * Extracts the name of an expanded element or attribute name.
 */
std::string ExtractElementName(const char* name) {
  const char* p = name;
  while (*p != 0 && *p != kXmlNamespaceSep) {
    p++;
  }

  std::string out_name;
  if (*p == 0) {
    out_name.assign(name);
  } else {
    out_name.assign(p + 1);
  }
  return out_name;
}

}  // namespace

void XMLCALL XmlTextParser::StartElementHandler(void* user_data, const char* name,
                                                const char** /*attrs*/) {
  XmlTextParser* parser = reinterpret_cast<XmlTextParser*>(user_data);

  EventData data{Event::kStartElement, ExtractElementName(name)};

  // Move the structure into the queue (no copy).
  parser->event_queue_.push(std::move(data));
}

void XMLCALL XmlTextParser::EndElementHandler(void* user_data, const char* name) {
  XmlTextParser* parser = reinterpret_cast<XmlTextParser*>(user_data);

  EventData data{Event::kEndElement, ExtractElementName(name)};

  // Move the data into the queue (no copy).
  parser->event_queue_.push(std::move(data));
}

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

#include <fstream>
#include <iostream>
#include <sstream>
#include <string>
#include <vector>

#include "gflags/gflags.h"
#include "io/FileStream.h"
#include "xml/XmlPullParser.h"

#include "java_builder.h"
#include "util.h"

using aapt::io::FileInputStream;
using aapt::xml::XmlPullParser;
using std::string;

constexpr char kStdoutFilename[]{"stdout"};

DEFINE_string(package, "", "The package name for the generated class (required)");
DEFINE_string(out, kStdoutFilename, "Where to write the generated class");

int main(int argc, char** argv) {
  constexpr size_t kProgramName = 0;
  constexpr size_t kFileNameParam = 1;
  constexpr size_t kNumRequiredArgs = 2;

  gflags::SetUsageMessage(
      "Compile XML layout files into equivalent Java code\n"
      "\n"
      "  example usage:  viewcompiler layout.xml --package com.example.androidapp");
  gflags::ParseCommandLineFlags(&argc, &argv, /*remove_flags*/ true);

  gflags::CommandLineFlagInfo cmd = gflags::GetCommandLineFlagInfoOrDie("package");
  if (argc != kNumRequiredArgs || cmd.is_default) {
    gflags::ShowUsageWithFlags(argv[kProgramName]);
    return 1;
  }

  const char* const filename = argv[kFileNameParam];
  const string layout_name = FindLayoutNameFromFilename(filename);

  // We want to generate Java code to inflate exactly this layout. This means
  // generating code to walk the resource XML too. A couple things are
  // different. The C++ version of XmlPullParser doesn't find any tag until
  // you call next(), while the Java version seems to start at the first tag.
  // Secondly, at runtime, on device, we are using binary XML that has had all
  // of its text and comment nodes stripped.

  FileInputStream file{filename};
  XmlPullParser xml{/*in*/ &file};

  std::ofstream outfile;
  if (FLAGS_out != kStdoutFilename) {
    outfile.open(FLAGS_out);
  }
  JavaViewBuilder builder{
      FLAGS_package, layout_name, FLAGS_out == kStdoutFilename ? std::cout : outfile};

  builder.Start();

  XmlPullParser::Event event = xml.event();
  do {
    // TODO: handle include, merge, etc.
    if (event == XmlPullParser::Event::kStartElement) {
      builder.StartView(xml.element_name());
    }

    if (event == XmlPullParser::Event::kEndElement) {
      builder.FinishView();
    }

  } while ((event = xml.Next()) != XmlPullParser::Event::kEndDocument);

  builder.Finish();

  return 0;
}
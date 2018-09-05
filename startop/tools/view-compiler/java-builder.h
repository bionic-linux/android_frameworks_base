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
#ifndef JAVA_BUILDER_H_
#define JAVA_BUILDER_H_

#include <iostream>
#include <sstream>
#include <vector>

// Build Java code to instantiate views.
//
// This has a very small interface to make it easier to generate additional
// backends, such as a direct-to-DEX version.
class JavaBuilder {
 public:
  JavaBuilder(std::string package, std::string layout_name, std::ostream& out = std::cout)
      : package_(package), layout_name_(layout_name), out_(out) {}

  void Begin() const;

  void End() const;

  void BeginView(const std::string& classname);

  void EndView();

 private:
  std::string const package_;
  std::string const layout_name_;

  std::ostream& out_;

  size_t view_id_ = 0;

  struct StackEntry {
      // The class name for this view object
      const std::string classname;

      // The variable name that is holding the view object
      const std::string view_var;

      // The variable name that holds the object's layout parameters
      const std::string layout_params_var;
  };
  std::vector<StackEntry> view_stack_;

  const std::string MakeVar(std::string prefix = "v");
};

#endif  // JAVA_BUILDER_H_

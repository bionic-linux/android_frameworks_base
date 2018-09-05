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

#include "java_builder.h"

using std::string;

void JavaBuilder::Start() const {
  out_ << "package " << package_
       << ";\n"
          "import android.content.Context;\n"
          "import android.content.res.Resources;\n"
          "import android.content.res.XmlResourceParser;\n"
          "import android.util.AttributeSet;\n"
          "import android.util.Xml;\n"
          "import android.view.*;\n"
          "import android.widget.*;\n"
          "\n"
          "public final class CompiledView {\n"
          "\n"
          "static <T extends View> T createView(Context context, AttributeSet attrs, View parent, "
          "String name, LayoutInflater.Factory factory, LayoutInflater.Factory2 factory2) {"
          "\n"
          "  if (factory2 != null) {\n"
          "    return (T)factory2.onCreateView(parent, name, context, attrs);\n"
          "  } else if (factory != null) {\n"
          "    return (T)factory.onCreateView(name, context, attrs);\n"
          "  }\n"
          // TODO: find a way to call the private factory
          "  return null;\n"
          "}\n"
          "\n"
          "  public static View inflate(Context context) {\n"
          "    try {\n"
          "      LayoutInflater inflater = LayoutInflater.from(context);\n"
          "      LayoutInflater.Factory factory = inflater.getFactory();\n"
          "      LayoutInflater.Factory2 factory2 = inflater.getFactory2();\n"
          "      Resources res = context.getResources();\n"
          "      XmlResourceParser xml = res.getLayout("
       << package_ << ".R.layout." << layout_name_
       << ");\n"
          "      AttributeSet attrs = Xml.asAttributeSet(xml);\n"

          // The Java XmlPullParser needs a call to next to find the start document tag.
          "      xml.next(); // start document\n";
}

void JavaBuilder::Finish() const {
  out_ << "    } catch (Exception e) {\n"
          "      return null;\n"
          "    }\n"  // end try
          "  }\n"    // end inflate
          "}\n";     // end CompiledView
}

void JavaBuilder::BeginView(const string& class_name) {
  const string var = MakeVar();
  const string layout = MakeVar("l");
  std::string parent = "null";
  if (!view_stack_.empty()) {
    const StackEntry& parent_entry = view_stack_.back();
    parent = parent_entry.view_var;
  }
  out_ << "      xml.next(); // <" << class_name
       << ">\n"
          "      "
       << class_name << " " << var << " = createView(context, attrs, " << parent << ", \""
       << class_name
       << "\", factory, factory2);\n"
          "      if ("
       << var << " == null) " << var << " = new " << class_name << "(context, attrs);\n";
  if (!view_stack_.empty()) {
    out_ << "      ViewGroup.LayoutParams " << layout << " = " << parent
         << ".generateLayoutParams(attrs)";
  }
  out_ << ";\n";
  view_stack_.push_back({class_name, var, layout});
}

void JavaBuilder::EndView() {
  const StackEntry var = view_stack_.back();
  view_stack_.pop_back();
  if (!view_stack_.empty()) {
    const string& parent = view_stack_.back().view_var;
    out_ << "      xml.next(); // </" << var.class_name
         << ">\n"
            "      "
         << parent << ".addView(" << var.view_var << ", " << var.layout_params_var << ");\n";
  } else {
    out_ << "      return " << var.view_var << ";\n";
  }
}

const std::string JavaBuilder::MakeVar(std::string prefix) {
  std::stringstream v;
  v << prefix << view_id_++;
  return v.str();
}

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

#include "java-builder.h"

using std::endl;
using std::string;

void JavaBuilder::Begin() const {
  out_ << "package " << package_ << ";" << endl;
  out_ << "import android.content.Context;" << endl;
  out_ << "import android.content.res.Resources;" << endl;
  out_ << "import android.content.res.XmlResourceParser;" << endl;
  out_ << "import android.util.AttributeSet;" << endl;
  out_ << "import android.util.Xml;" << endl;
  out_ << "import android.view.*;" << endl;
  out_ << "import android.widget.*;" << endl;
  out_ << endl;
  out_ << "public final class CompiledView {" << endl;
  out_ << endl;
  out_ << "static <T extends View> T createView(Context context, AttributeSet attrs, View parent, "
          "String name, LayoutInflater.Factory factory, LayoutInflater.Factory2 factory2) {"
       << endl;
  out_ << "  if (factory2 != null) {" << endl;
  out_ << "    return (T)factory2.onCreateView(parent, name, context, attrs);" << endl;
  out_ << "  } else if (factory != null) {" << endl;
  out_ << "    return (T)factory.onCreateView(name, context, attrs);" << endl;
  out_ << "  }" << endl;
  // TODO: find a way to call the private factory
  out_ << "  return null;" << endl;
  out_ << "}" << endl;
  out_ << endl;
  out_ << "  public static View inflate(Context context) {" << endl;
  out_ << "    try {" << endl;
  out_ << "      LayoutInflater inflater = LayoutInflater.from(context);" << endl;
  out_ << "      LayoutInflater.Factory factory = inflater.getFactory();" << endl;
  out_ << "      LayoutInflater.Factory2 factory2 = inflater.getFactory2();" << endl;
  out_ << "      Resources res = context.getResources();" << endl;
  out_ << "      XmlResourceParser xml = res.getLayout(" << package_ << ".R.layout." << layout_name_
       << ");" << endl;
  out_ << "      AttributeSet attrs = Xml.asAttributeSet(xml);" << endl;

  // The Java XmlPullParser needs a call to next to find the start document tag.
  out_ << "      xml.next(); // start document" << endl;
}

void JavaBuilder::End() const {
  out_ << "    } catch (Exception e) {" << endl;
  out_ << "      return null;" << endl;
  out_ << "    }" << endl;  // end try
  out_ << "  }" << endl;    // end inflate
  out_ << "}" << endl;      // end CompiledView
}

void JavaBuilder::BeginView(const string& classname) {
  const auto var = MakeVar();
  const auto layout = MakeVar("l");
  std::string parent = "null";
  if (view_stack_.size() > 0) {
    const auto& parent_entry = view_stack_.back();
    parent = parent_entry.view_var;
  }
  out_ << "      xml.next(); // <" << classname << ">" << endl;
  out_ << "      " << classname << " " << var << " = createView(context, attrs, " << parent
       << ", \"" << classname << "\", factory, factory2);" << endl;
  out_ << "      if (" << var << " == null) " << var << " = new " << classname
       << "(context, attrs);" << endl;
  if (view_stack_.size() > 0) {
    out_ << "      ViewGroup.LayoutParams " << layout << " = ";
    out_ << parent << ".generateLayoutParams(attrs)";
  }
  out_ << ";" << endl;
  view_stack_.push_back({classname, var, layout});
}

void JavaBuilder::EndView() {
  const auto var = view_stack_.back();
  view_stack_.pop_back();
  if (view_stack_.size() > 0) {
    out_ << "      xml.next(); // </" << var.classname << ">" << endl;
    const auto parent = view_stack_.back().view_var;
    out_ << "      " << parent << ".addView(" << var.view_var << ", " << var.layout_params_var
         << ");" << endl;
  } else {
    out_ << "      return " << var.view_var << ";" << endl;
  }
}

const std::string JavaBuilder::MakeVar(std::string prefix) {
  std::stringstream v;
  v << prefix << view_id_++;
  return v.str();
}

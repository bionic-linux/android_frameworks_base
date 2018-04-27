#include <algorithm>
#include <iomanip>
#include <iostream>
#include <set>

#include "android-base/macros.h"

#include "idmap2/CommandLineOptions.h"

namespace android {
namespace idmap2 {

std::unique_ptr<std::vector<std::string>> CommandLineOptions::ConvertArgvToVector(
    int argc, const char** argv) {
  std::unique_ptr<std::vector<std::string>> v(new std::vector<std::string>());
  for (int i = 1; i < argc; i++) {  // skip argv[0] == path to binary
    v->push_back(argv[i]);
  }
  return v;
}

CommandLineOptions& CommandLineOptions::OptionalFlag(const std::string& name,
                                                     const std::string& description, bool* value) {
  auto func = [value](const std::string& arg ATTRIBUTE_UNUSED) -> void { *value = true; };
  options_.push_back(Option{name, description, func, Option::COUNT_OPTIONAL, false});
  return *this;
}

CommandLineOptions& CommandLineOptions::MandatoryOption(const std::string& name,
                                                        const std::string& description,
                                                        std::string* value) {
  auto func = [value](const std::string& arg) -> void { *value = arg; };
  options_.push_back(Option{name, description, func, Option::COUNT_EXACTLY_ONCE, true});
  return *this;
}

CommandLineOptions& CommandLineOptions::MandatoryOption(const std::string& name,
                                                        const std::string& description,
                                                        std::vector<std::string>* value) {
  auto func = [value](const std::string& arg) -> void { value->push_back(arg); };
  options_.push_back(Option{name, description, func, Option::COUNT_ONCE_OR_MORE, true});
  return *this;
}

CommandLineOptions& CommandLineOptions::OptionalOption(const std::string& name,
                                                       const std::string& description,
                                                       std::string* value) {
  auto func = [value](const std::string& arg) -> void { *value = arg; };
  options_.push_back(Option{name, description, func, Option::COUNT_OPTIONAL, true});
  return *this;
}

bool CommandLineOptions::Parse(const std::vector<std::string>& argv, std::ostream& outError) const {
  // This is required to keep the function as const (the alternative is to
  // update Option.mandatory during parsing).
  std::set<std::string> parsedArguments;

  for (size_t i = 0; i < argv.size(); i++) {
    const std::string arg = argv[i];
    if ("--help" == arg || "-h" == arg) {
      Usage(outError);
      return false;
    }
    bool match = false;
    for (const Option& opt : options_) {
      if (opt.name == arg) {
        match = true;

        if (opt.argument) {
          i++;
          if (i >= argv.size()) {
            outError << "error: " << opt.name << ": missing argument" << std::endl;
            Usage(outError);
            return false;
          }
        }
        opt.action(argv[i]);
        parsedArguments.emplace(opt.name);
        break;
      }
    }
    if (!match) {
      outError << "error: " << arg << ": unknown option" << std::endl;
      Usage(outError);
      return false;
    }
  }
  for (const Option& opt : options_) {
    const bool mandatory = opt.count != Option::COUNT_OPTIONAL;
    if (mandatory && parsedArguments.find(opt.name) == parsedArguments.end()) {
      outError << "error: " << opt.name << ": missing mandatory option" << std::endl;
      Usage(outError);
      return false;
    }
  }
  return true;
}

void CommandLineOptions::Usage(std::ostream& out) const {
  size_t maxLength = 0;
  out << "usage: " << name_;
  for (const Option& opt : options_) {
    const bool mandatory = opt.count != Option::COUNT_OPTIONAL;
    out << " ";
    if (!mandatory) {
      out << "[";
    }
    if (opt.argument) {
      out << opt.name << " arg";
      maxLength = std::max(maxLength, opt.name.size() + 4);
    } else {
      out << opt.name;
      maxLength = std::max(maxLength, opt.name.size());
    }
    if (!mandatory) {
      out << "]";
    }
    if (opt.count == Option::COUNT_ONCE_OR_MORE) {
      out << " [" << opt.name << " arg [..]]";
    }
  }
  out << std::endl << std::endl;
  for (const Option& opt : options_) {
    out << std::left << std::setw(maxLength);
    if (opt.argument) {
      out << (opt.name + " arg");
    } else {
      out << opt.name;
    }
    out << "    " << opt.description;
    if (opt.count == Option::COUNT_ONCE_OR_MORE) {
      out << " (can be provided multiple times)";
    }
    out << std::endl;
  }
}

}  // namespace idmap2
}  // namespace android

#ifndef IDMAP2_COMMAND_LINE_OPTIONS_H
#define IDMAP2_COMMAND_LINE_OPTIONS_H

#include <functional>
#include <memory>
#include <ostream>
#include <string>
#include <vector>

namespace android {
namespace idmap2 {

class CommandLineOptions {
 public:
  static std::unique_ptr<std::vector<std::string>> ConvertArgvToVector(int argc, const char** argv);

  CommandLineOptions(const std::string& name) : name_(name) {
  }

  CommandLineOptions& OptionalFlag(const std::string& name, const std::string& description,
                                   bool* value);
  CommandLineOptions& MandatoryOption(const std::string& name, const std::string& description,
                                      std::string* value);
  CommandLineOptions& MandatoryOption(const std::string& name, const std::string& description,
                                      std::vector<std::string>* value);
  CommandLineOptions& OptionalOption(const std::string& name, const std::string& description,
                                     std::string* value);
  bool Parse(const std::vector<std::string>& argv, std::ostream& outError) const;
  void Usage(std::ostream& out) const;

 private:
  struct Option {
    std::string name;
    std::string description;
    std::function<void(const std::string& value)> action;
    enum {
      COUNT_OPTIONAL,
      COUNT_EXACTLY_ONCE,
      COUNT_ONCE_OR_MORE,
    } count;
    bool argument;
  };

  std::vector<Option> options_;
  std::string name_;
};

}  // namespace idmap2
}  // namespace android

#endif

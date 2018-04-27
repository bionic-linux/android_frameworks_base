#ifndef IDMAP2_XML_H
#define IDMAP2_XML_H

#include <map>
#include <memory>
#include <string>

#include "android-base/macros.h"
#include "androidfw/ResourceTypes.h"
#include "utils/String16.h"

namespace android {
namespace idmap2 {

class Xml {
 public:
  static std::unique_ptr<const Xml> Create(const uint8_t* data, size_t size, bool copyData = false);

  std::unique_ptr<std::map<std::string, std::string>> FindTag(const std::string& name) const;

  ~Xml();

 private:
  DISALLOW_COPY_AND_ASSIGN(Xml);
  Xml() {
  }

  mutable ResXMLTree xml_;
};

}  // namespace idmap2
}  // namespace android

#endif

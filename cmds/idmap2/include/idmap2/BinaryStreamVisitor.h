#ifndef IDMAP2_BINARYSTREAMVISITOR_H
#define IDMAP2_BINARYSTREAMVISITOR_H

#include <cstdint>
#include <iostream>

#include "idmap2/Idmap.h"

namespace android {
namespace idmap2 {

class BinaryStreamVisitor : public Visitor {
 public:
  BinaryStreamVisitor(std::ostream& stream) : stream_(stream) {
  }
  virtual void visit(const Idmap& idmap);
  virtual void visit(const IdmapHeader& header);
  virtual void visit(const IdmapData& data);
  virtual void visit(const IdmapData::Header& header);
  virtual void visit(const IdmapData::ResourceType& resourceType);

 private:
  void Write16(uint16_t value);
  void Write32(uint32_t value);
  void WriteString(const std::string& value);
  std::ostream& stream_;
};

}  // namespace idmap2
}  // namespace android

#endif

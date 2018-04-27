#ifndef IDMAP2_RAWPRINTVISITOR_H
#define IDMAP2_RAWPRINTVISITOR_H

#include <iostream>
#include <memory>

#include "androidfw/AssetManager2.h"

#include "idmap2/Idmap.h"

namespace android {

class ApkAssets;

namespace idmap2 {

class RawPrintVisitor : public Visitor {
 public:
  RawPrintVisitor(std::ostream& stream) : stream_(stream), offset_(0) {
  }
  virtual void visit(const Idmap& idmap);
  virtual void visit(const IdmapHeader& header);
  virtual void visit(const IdmapData& data);
  virtual void visit(const IdmapData::Header& header);
  virtual void visit(const IdmapData::ResourceType& resourceType);

 private:
  void print(uint16_t value, const char* fmt, ...);
  void print(uint32_t value, const char* fmt, ...);
  void print(const std::string& value, const char* fmt, ...);

  std::ostream& stream_;
  std::unique_ptr<const ApkAssets> target_apk_;
  AssetManager2 target_am_;
  size_t offset_;
  uint32_t last_seen_package_id_;
};

}  // namespace idmap2
}  // namespace android

#endif

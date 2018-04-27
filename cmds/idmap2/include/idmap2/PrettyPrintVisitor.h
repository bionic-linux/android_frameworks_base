#ifndef IDMAP2_PRETTYPRINTVISITOR_H
#define IDMAP2_PRETTYPRINTVISITOR_H

#include <iostream>
#include <memory>

#include "androidfw/AssetManager2.h"

#include "idmap2/Idmap.h"

namespace android {

class ApkAssets;

namespace idmap2 {

class PrettyPrintVisitor : public Visitor {
 public:
  PrettyPrintVisitor(std::ostream& stream) : stream_(stream) {
  }
  virtual void visit(const Idmap& idmap);
  virtual void visit(const IdmapHeader& header);
  virtual void visit(const IdmapData& data);
  virtual void visit(const IdmapData::Header& header);
  virtual void visit(const IdmapData::ResourceType& resourceType);

 private:
  std::ostream& stream_;
  std::unique_ptr<const ApkAssets> target_apk_;
  AssetManager2 target_am_;
  uint32_t last_seen_package_id_;
};

}  // namespace idmap2
}  // namespace android

#endif

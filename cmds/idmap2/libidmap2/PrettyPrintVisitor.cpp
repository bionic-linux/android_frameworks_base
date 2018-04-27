#include "android-base/macros.h"
#include "android-base/stringprintf.h"
#include "androidfw/ApkAssets.h"

#include "idmap2/PrettyPrintVisitor.h"
#include "idmap2/ResourceUtils.h"

namespace android {
namespace idmap2 {

#define RESID(pkg, type, entry) (((pkg) << 24) | ((type) << 16) | (entry))

void PrettyPrintVisitor::visit(const Idmap& idmap ATTRIBUTE_UNUSED) {
}

void PrettyPrintVisitor::visit(const IdmapHeader& header) {
  // FIXME: read and parse manifest of overlay to check if isStatic, priority, target and overlay
  // package names?
  stream_ << "target apk path  : " << header.GetTargetPath() << std::endl
          << "overlay apk path : " << header.GetOverlayPath() << std::endl;

  target_apk_ = ApkAssets::Load(header.GetTargetPath());
  if (target_apk_) {
    target_am_.SetApkAssets({target_apk_.get()});
  }
}

void PrettyPrintVisitor::visit(const IdmapData& data ATTRIBUTE_UNUSED) {
}

void PrettyPrintVisitor::visit(const IdmapData::Header& header ATTRIBUTE_UNUSED) {
  last_seen_package_id_ = header.GetTargetPackageId();
}

void PrettyPrintVisitor::visit(const IdmapData::ResourceType& rt) {
  const bool target_package_loaded = !target_am_.GetApkAssets().empty();
  for (uint16_t i = 0; i < rt.GetEntryCount(); i++) {
    const uint32_t entry = rt.GetEntry(i);
    if (entry == 0xffffffff) {
      continue;
    }

    const uint32_t target_resid =
        RESID(last_seen_package_id_, rt.GetTargetType(), rt.GetEntryOffset() + i);
    const uint32_t overlay_resid = RESID(last_seen_package_id_, rt.GetOverlayType(), entry);

    stream_ << base::StringPrintf("0x%08x -> 0x%08x", target_resid, overlay_resid);
    if (target_package_loaded) {
      std::string name;
      if (utils::ResidToQualifiedName(target_am_, target_resid, name)) {
        stream_ << " " << name;
      }
    }
    stream_ << std::endl;
  }
}

}  // namespace idmap2
}  // namespace android

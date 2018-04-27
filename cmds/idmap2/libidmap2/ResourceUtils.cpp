#include "idmap2/ResourceUtils.h"

namespace android {
namespace idmap2 {
namespace utils {

bool WARN_UNUSED ResidToQualifiedName(const AssetManager2& am, uint32_t resid, std::string& out) {
  AssetManager2::ResourceName name;
  if (!am.GetResourceName(resid, &name)) {
    return false;
  }
  out.clear();
  if (name.type != nullptr) {
    out.append(name.type, name.type_len);
  } else {
    String16 str16(name.type16, name.type_len);
    String8 str8(str16);
    out.append(str8.string());
  }
  out.append("/");
  if (name.entry != nullptr) {
    out.append(name.entry, name.entry_len);
  } else {
    String16 str16(name.entry16, name.entry_len);
    String8 str8(str16);
    out.append(str8.string());
  }
  return true;
}

}  // namespace utils
}  // namespace idmap2
}  // namespace android

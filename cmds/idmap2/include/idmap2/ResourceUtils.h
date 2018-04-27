#ifndef IDMAP2_RESOURCEUTILS_H
#define IDMAP2_RESOURCEUTILS_H

#include <string>

#include "android-base/macros.h"
#include "androidfw/AssetManager2.h"

namespace android {
namespace idmap2 {
namespace utils {

bool WARN_UNUSED ResidToQualifiedName(const AssetManager2& am, uint32_t resid, std::string& out);

}  // namespace utils
}  // namespace idmap2
}  // namespace android

#endif

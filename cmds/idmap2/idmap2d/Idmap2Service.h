#ifndef IDMAP2_IDMAP2D_IDMAP2SERVICE
#define IDMAP2_IDMAP2D_IDMAP2SERVICE

#include <android-base/unique_fd.h>
#include <binder/BinderService.h>

#include "android/os/BnIdmap2.h"

namespace android {
namespace os {
class Idmap2Service : public BinderService<Idmap2Service>, public BnIdmap2 {
 public:
  static char const* getServiceName() {
    return "idmap2d";
  }

  binder::Status getIdmapPath(const std::string& overlay_apk_path, int32_t user_id,
                              std::string* _aidl_return);

  binder::Status removeIdmap(const std::string& overlay_apk_path, int32_t user_id,
                             bool* _aidl_return);

  binder::Status createIdmap(const std::string& target_apk_path,
                             const std::string& overlay_apk_path, bool ignore_categories,
                             int32_t user_id, std::unique_ptr<std::string>* _aidl_return);
};
}  // namespace os
}  // namespace android

#endif

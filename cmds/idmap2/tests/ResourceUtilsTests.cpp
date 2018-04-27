#include <memory>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "androidfw/ApkAssets.h"
#include "idmap2/ResourceUtils.h"

#include "TestHelpers.h"

using ::testing::NotNull;

namespace android {
namespace idmap2 {

class ResourceUtilsTests : public Idmap2Tests {
 protected:
  virtual void SetUp() override {
    Idmap2Tests::SetUp();

    apk_assets_ = ApkAssets::Load(GetTargetApkPath());
    ASSERT_THAT(apk_assets_, NotNull());

    am_.SetApkAssets({apk_assets_.get()});
  }

  const AssetManager2& GetAssetManager() {
    return am_;
  }

 private:
  AssetManager2 am_;
  std::unique_ptr<const ApkAssets> apk_assets_;
};

TEST_F(ResourceUtilsTests, ResidToQualifiedName) {
  std::string name;
  ASSERT_TRUE(utils::ResidToQualifiedName(GetAssetManager(), 0x7f010000u, name));
  ASSERT_EQ(name, "integer/int1");

  ASSERT_FALSE(utils::ResidToQualifiedName(GetAssetManager(), 0x7f123456u, name));
}

}  // namespace idmap2
}  // namespace android

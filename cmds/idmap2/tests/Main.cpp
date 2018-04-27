#include "android-base/file.h"

#include "gtest/gtest.h"

#include "TestHelpers.h"

namespace android {
namespace idmap2 {

static std::string sTestDataPath;

const std::string& GetTestDataPath() {
  return sTestDataPath;
}

void InitializeTest() {
  sTestDataPath = base::GetExecutableDirectory() + "/tests/data";
}

}  // namespace idmap2
}  // namespace android

int main(int argc, char** argv) {
  ::testing::InitGoogleTest(&argc, argv);
  ::android::idmap2::InitializeTest();

  return RUN_ALL_TESTS();
}

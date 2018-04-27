#include <dirent.h>
#include <set>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "android-base/macros.h"

#include "idmap2/FileUtils.h"

#include "TestHelpers.h"

using ::testing::NotNull;

namespace android {
namespace idmap2 {
namespace utils {

TEST(FileUtilsTests, FindFilesFindEverythingNonRecursive) {
  const auto& root = GetTestDataPath();
  auto v = utils::FindFiles(root, false,
                            [](unsigned char type ATTRIBUTE_UNUSED,
                               const std::string& path ATTRIBUTE_UNUSED) -> bool { return true; });
  ASSERT_THAT(v, NotNull());
  ASSERT_EQ(v->size(), 4u);
  ASSERT_EQ(
      std::set<std::string>(v->begin(), v->end()),
      std::set<std::string>({root + "/.", root + "/..", root + "/overlay", root + "/target"}));
}

TEST(FileUtilsTests, FindFilesFindApkFilesRecursive) {
  const auto& root = GetTestDataPath();
  auto v = utils::FindFiles(root, true, [](unsigned char type, const std::string& path) -> bool {
    return type == DT_REG && path.size() > 4 && !path.compare(path.size() - 4, 4, ".apk");
  });
  ASSERT_THAT(v, NotNull());
  ASSERT_EQ(v->size(), 4u);
  ASSERT_EQ(std::set<std::string>(v->begin(), v->end()),
            std::set<std::string>({root + "/target/target.apk", root + "/overlay/overlay.apk",
                                   root + "/overlay/overlay-static-1.apk",
                                   root + "/overlay/overlay-static-2.apk"}));
}

TEST(FileUtilsTests, ReadFile) {
  int pipefd[2];
  ASSERT_EQ(pipe(pipefd), 0);

  ASSERT_EQ(write(pipefd[1], "foobar", 6), 6);
  close(pipefd[1]);

  auto data = ReadFile(pipefd[0]);
  ASSERT_THAT(data, NotNull());
  ASSERT_EQ(*data, "foobar");
  close(pipefd[0]);
}

}  // namespace utils
}  // namespace idmap2
}  // namespace android

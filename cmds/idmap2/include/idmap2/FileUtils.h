#ifndef IDMAP2_FILEUTILS_H
#define IDMAP2_FILEUTILS_H

#include <memory>
#include <string>
#include <vector>

namespace android {
namespace idmap2 {
namespace utils {

typedef bool (*FindFilesPredicate)(unsigned char type,  // DT_* from dirent.h
                                   const std::string& path);
std::unique_ptr<std::vector<std::string>> FindFiles(const std::string& root, bool recurse,
                                                    const FindFilesPredicate& predicate);

std::unique_ptr<std::string> ReadFile(int fd);

std::unique_ptr<std::string> ReadFile(const std::string& path);

}  // namespace utils
}  // namespace idmap2
}  // namespace android

#endif

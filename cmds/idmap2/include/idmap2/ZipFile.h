#ifndef IDMAP2_ZIPFILE_H
#define IDMAP2_ZIPFILE_H

#include <memory>
#include <string>

#include "android-base/macros.h"
#include "ziparchive/zip_archive.h"

namespace android {
namespace idmap2 {

struct MemoryChunk {
  size_t size;
  uint8_t buf[0];
};

class ZipFile {
 public:
  static std::unique_ptr<const ZipFile> Open(const std::string& path);

  std::unique_ptr<const MemoryChunk> Uncompress(const std::string& entryPath) const;
  bool Crc(const std::string& entryPath, uint32_t& out) const;

  ~ZipFile();

 private:
  DISALLOW_COPY_AND_ASSIGN(ZipFile);
  ZipFile(const ::ZipArchiveHandle handle) : handle_(handle) {
  }

  const ::ZipArchiveHandle handle_;
};

}  // namespace idmap2
}  // namespace android

#endif

#include "idmap2/ZipFile.h"

namespace android {
namespace idmap2 {

std::unique_ptr<const ZipFile> ZipFile::Open(const std::string& path) {
  ::ZipArchiveHandle handle;
  int32_t status = ::OpenArchive(path.c_str(), &handle);
  if (status != 0) {
    return nullptr;
  }
  return std::unique_ptr<ZipFile>(new ZipFile(handle));
}

ZipFile::~ZipFile() {
  ::CloseArchive(handle_);
}

std::unique_ptr<const MemoryChunk> ZipFile::Uncompress(const std::string& entryPath) const {
  ::ZipEntry entry;
  int32_t status = ::FindEntry(handle_, ::ZipString(entryPath.c_str()), &entry);
  if (status != 0) {
    return nullptr;
  }
  void* ptr = ::operator new(sizeof(MemoryChunk) + entry.uncompressed_length);
  std::unique_ptr<MemoryChunk> chunk(reinterpret_cast<MemoryChunk*>(ptr));
  chunk->size = entry.uncompressed_length;
  status = ::ExtractToMemory(handle_, &entry, chunk->buf, chunk->size);
  if (status != 0) {
    return nullptr;
  }
  return chunk;
}

bool ZipFile::Crc(const std::string& entryPath, uint32_t& out) const {
  ::ZipEntry entry;
  int32_t status = ::FindEntry(handle_, ::ZipString(entryPath.c_str()), &entry);
  if (status != 0) {
    return false;
  }
  out = entry.crc32;
  return true;
}

}  // namespace idmap2
}  // namespace android

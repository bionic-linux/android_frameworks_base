#include <cstring>

#include "android-base/macros.h"

#include "idmap2/BinaryStreamVisitor.h"

namespace android {
namespace idmap2 {

void BinaryStreamVisitor::Write16(uint16_t value) {
  uint16_t x = dtohl(value);
  stream_.write((char*)&x, sizeof(uint16_t));
}

void BinaryStreamVisitor::Write32(uint32_t value) {
  uint32_t x = dtohl(value);
  stream_.write((char*)&x, sizeof(uint32_t));
}

void BinaryStreamVisitor::WriteString(const std::string& value) {
  // FIXME: check length no longer than 256
  char buf[256 + 1];
  memset(buf, 0, sizeof(buf));
  memcpy(buf, value.c_str(), std::min(value.size(), sizeof(buf) - 1));
  stream_.write(buf, sizeof(buf) - 1);
}

void BinaryStreamVisitor::visit(const Idmap& idmap ATTRIBUTE_UNUSED) {
  // nothing to do
}

void BinaryStreamVisitor::visit(const IdmapHeader& header) {
  Write32(header.GetMagic());
  Write32(header.GetVersion());
  Write32(header.GetTargetCrc());
  Write32(header.GetOverlayCrc());
  WriteString(header.GetTargetPath());
  WriteString(header.GetOverlayPath());
}

void BinaryStreamVisitor::visit(const IdmapData& data ATTRIBUTE_UNUSED) {
  // nothing to do
}

void BinaryStreamVisitor::visit(const IdmapData::Header& header) {
  Write16(header.GetTargetPackageId());
  Write16(header.GetTypeCount());
}

void BinaryStreamVisitor::visit(const IdmapData::ResourceType& rt) {
  const uint16_t entryCount = rt.GetEntryCount();

  Write16(rt.GetTargetType());
  Write16(rt.GetOverlayType());
  Write16(entryCount);
  Write16(rt.GetEntryOffset());
  for (uint16_t i = 0; i < entryCount; i++) {
    Write32(rt.GetEntry(i));
  }
}

}  // namespace idmap2
}  // namespace android

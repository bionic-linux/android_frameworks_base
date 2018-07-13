/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef IDMAP2_IDMAP_H_
#define IDMAP2_IDMAP_H_

#include <iostream>
#include <memory>
#include <string>
#include <vector>

#include "android-base/macros.h"

#include "androidfw/ApkAssets.h"
#include "androidfw/ResourceTypes.h"
#include "androidfw/StringPiece.h"

namespace android {
namespace idmap2 {

class Idmap;
class Visitor;

class IdmapHeader {
 public:
  static std::unique_ptr<const IdmapHeader> FromBinaryStream(std::istream& stream);

  inline uint32_t GetMagic() const {
    return magic_;
  }

  inline uint32_t GetVersion() const {
    return version_;
  }

  inline uint32_t GetTargetCrc() const {
    return target_crc_;
  }

  inline uint32_t GetOverlayCrc() const {
    return overlay_crc_;
  }

  inline const std::string& GetTargetPath() const {
    return target_path_;
  }

  inline const std::string& GetOverlayPath() const {
    return overlay_path_;
  }

  void accept(Visitor& v) const;

 private:
  DISALLOW_COPY_AND_ASSIGN(IdmapHeader);
  IdmapHeader() {  // FIXME: consider default values for easier call from
                   // Idmap::FromApkAssets
  }

  uint32_t magic_;
  uint32_t version_;
  uint32_t target_crc_;
  uint32_t overlay_crc_;
  std::string target_path_;
  std::string overlay_path_;

  friend Idmap;
};

class IdmapData {
 public:
  class Header {
   public:
    static std::unique_ptr<const Header> FromBinaryStream(std::istream& stream);

    inline uint8_t GetTargetPackageId() const {
      return target_package_id_;
    }

    inline uint16_t GetTypeCount() const {
      return type_count_;
    }

    void accept(Visitor& v) const;

   private:
    DISALLOW_COPY_AND_ASSIGN(Header);
    Header() {
    }

    uint8_t target_package_id_;
    uint16_t type_count_;
    friend Idmap;
  };

  class ResourceType {
   public:
    static std::unique_ptr<const ResourceType> FromBinaryStream(std::istream& stream);

    inline uint8_t GetTargetType() const {
      return target_type_;
    }

    inline uint8_t GetOverlayType() const {
      return overlay_type_;
    }

    inline uint16_t GetEntryCount() const {
      return entries_.size();
    }

    inline uint16_t GetEntryOffset() const {
      return entry_offset_;
    }

    inline uint32_t GetEntry(size_t i) const {
      return i < entries_.size() ? entries_[i] : 0xffffffff;
    }

    void accept(Visitor& v) const;

   private:
    DISALLOW_COPY_AND_ASSIGN(ResourceType);
    ResourceType() {
    }

    // FIXME: consider typedefs for package id, type id, entry id instead of raw uint_*
    uint8_t target_type_;
    uint8_t overlay_type_;
    uint16_t entry_offset_;
    std::vector<uint32_t> entries_;
    friend Idmap;
  };

  static std::unique_ptr<const IdmapData> FromBinaryStream(std::istream& stream);

  inline const std::unique_ptr<const Header>& GetHeader() const {
    return header_;
  }

  inline const std::vector<std::unique_ptr<const ResourceType>>& GetResourceTypes() const {
    return resource_types_;
  }

  void accept(Visitor& v) const;

 private:
  DISALLOW_COPY_AND_ASSIGN(IdmapData);
  IdmapData() {
  }

  std::unique_ptr<const Header> header_;
  std::vector<std::unique_ptr<const ResourceType>> resource_types_;
  friend Idmap;
};

class Idmap {
 public:
  // FIXME: magic and version should be accessbile from outside libidmap2 (for idmap2_tests)
  constexpr const static uint32_t magic = kIdmapMagic;
  constexpr const static uint32_t version = kIdmapCurrentVersion;

  static std::string CanonicalIdmapPathFor(const std::string& dir, const std::string& apk_path);

  static std::unique_ptr<const Idmap> FromBinaryStream(std::istream& stream,
                                                       std::ostream& out_error);

  // TODO: in the current version of idmap, the first package in each
  // resources.arsc file is used; change this in the next version of idmap to
  // use a named package instead; also update FromApkAssets to take additional
  // parameters: the target and overlay package names
  static std::unique_ptr<const Idmap> FromApkAssets(const std::string& target_apk_path,
                                                    const ApkAssets& target_apk_assets,
                                                    const std::string& overlay_apk_path,
                                                    const ApkAssets& overlay_apk_assets,
                                                    bool ignore_categories,
                                                    std::ostream& out_error);

  inline const std::unique_ptr<const IdmapHeader>& GetHeader() const {
    return header_;
  }

  inline const std::vector<std::unique_ptr<const IdmapData>>& GetData() const {
    return data_;
  }

  void accept(Visitor& v) const;

 private:
  DISALLOW_COPY_AND_ASSIGN(Idmap);
  Idmap() {
  }

  std::unique_ptr<const IdmapHeader> header_;
  std::vector<std::unique_ptr<const IdmapData>> data_;
};

class Visitor {
 public:
  virtual ~Visitor() {
  }
  virtual void visit(const Idmap& idmap) = 0;
  virtual void visit(const IdmapHeader& header) = 0;
  virtual void visit(const IdmapData& data) = 0;
  virtual void visit(const IdmapData::Header& header) = 0;
  virtual void visit(const IdmapData::ResourceType& resourceType) = 0;
};

}  // namespace idmap2
}  // namespace android
#endif

#include <sys/stat.h>   // umask
#include <sys/types.h>  // umask
#include <fstream>
#include <ostream>
#include <sstream>
#include <string>

#include "idmap2/BinaryStreamVisitor.h"
#include "idmap2/CommandLineOptions.h"
#include "idmap2/FileUtils.h"
#include "idmap2/Idmap.h"

using android::ApkAssets;
using android::idmap2::BinaryStreamVisitor;
using android::idmap2::CommandLineOptions;
using android::idmap2::Idmap;

int Create(const std::vector<std::string>& args, std::ostream& out_error) {
  std::string target_apk_path, overlay_apk_path, idmap_path;
  bool ignore_categories;

  const CommandLineOptions opts =
      CommandLineOptions("idmap2 create")
          .MandatoryOption("--target-apk-path",
                           "input: path to apk which will have its resources overlaid",
                           &target_apk_path)
          .MandatoryOption("--overlay-apk-path",
                           "input: path to apk which contains the new resource values",
                           &overlay_apk_path)
          .MandatoryOption("--idmap-path", "output: path to where to write idmap file", &idmap_path)
          .OptionalFlag("--ignore-categories", "do not limit matches by overlay category",
                        &ignore_categories);
  if (!opts.Parse(args, out_error)) {
    return 1;
  }

  const std::unique_ptr<const ApkAssets> target_apk = ApkAssets::Load(target_apk_path);
  if (!target_apk) {
    out_error << "error: failed to load apk " << target_apk_path << std::endl;
    return 1;
  }

  const std::unique_ptr<const ApkAssets> overlay_apk = ApkAssets::Load(overlay_apk_path);
  if (!overlay_apk) {
    out_error << "error: failed to load apk " << overlay_apk_path << std::endl;
    return 1;
  }

  const std::unique_ptr<const Idmap> idmap = Idmap::FromApkAssets(
      target_apk_path, *target_apk, overlay_apk_path, *overlay_apk, ignore_categories, out_error);
  if (!idmap) {
    return 1;
  }

  umask(0122);
  std::ofstream fout(idmap_path);
  if (fout.fail()) {
    out_error << "failed to open idmap path " << idmap_path << std::endl;
    return 1;
  }
  BinaryStreamVisitor visitor(fout);
  idmap->accept(visitor);
  fout.close();
  if (fout.fail()) {
    out_error << "failed to write to idmap path " << idmap_path << std::endl;
    return 1;
  }

  return 0;
}

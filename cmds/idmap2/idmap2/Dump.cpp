#include <fstream>
#include <iostream>
#include <string>

#include "idmap2/CommandLineOptions.h"
#include "idmap2/Idmap.h"
#include "idmap2/PrettyPrintVisitor.h"
#include "idmap2/RawPrintVisitor.h"

using android::idmap2::CommandLineOptions;
using android::idmap2::Idmap;
using android::idmap2::PrettyPrintVisitor;
using android::idmap2::RawPrintVisitor;

int Dump(const std::vector<std::string>& args, std::ostream& out_error) {
  std::string idmap_path;
  bool verbose;

  const CommandLineOptions opts =
      CommandLineOptions("idmap2 dump")
          .MandatoryOption("--idmap-path", "input: path to idmap file to pretty-print", &idmap_path)
          .OptionalFlag("--verbose", "annotate every byte of the idmap", &verbose);
  if (!opts.Parse(args, out_error)) {
    return 1;
  }
  std::ifstream fin(idmap_path);
  const std::unique_ptr<const Idmap> idmap = Idmap::FromBinaryStream(fin, out_error);
  fin.close();
  if (!idmap) {
    return 1;
  }

  if (verbose) {
    RawPrintVisitor visitor(std::cout);
    idmap->accept(visitor);
  } else {
    PrettyPrintVisitor visitor(std::cout);
    idmap->accept(visitor);
  }

  return 0;
}

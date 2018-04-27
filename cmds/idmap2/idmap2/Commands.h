#ifndef IDMAP2_IDMAP2_COMMANDS_H
#define IDMAP2_IDMAP2_COMMANDS_H

int Create(const std::vector<std::string>& args, std::ostream& out_error);
int Dump(const std::vector<std::string>& args, std::ostream& out_error);
int Lookup(const std::vector<std::string>& args, std::ostream& out_error);
int Scan(const std::vector<std::string>& args, std::ostream& out_error);

#endif

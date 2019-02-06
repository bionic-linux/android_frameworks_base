#ifndef SOURCEPOS_H
#define SOURCEPOS_H

#include <stdio.h>
#include <utils/String8.h>

using namespace android;

class SourcePos {
  public:
    String8 file;
    int line;

    SourcePos(const String8& f, int l);
    SourcePos(const SourcePos& that);
    SourcePos();
    ~SourcePos();

    void error(const char* fmt, ...) const;
    void warning(const char* fmt, ...) const;
    void printf(const char* fmt, ...) const;

    bool operator<(const SourcePos& rhs) const;

    static bool hasErrors();
    static void printErrors(FILE* to);
};

#endif  // SOURCEPOS_H

/*
 * This is a parser library for parsing the packages.list file generated
 * by PackageManager service.
 *
 * This simple parser is sensitive to format changes in
 * frameworks/base/services/core/java/com/android/server/pm/Settings.java
 * A dependency note has been added to that file to correct
 * this parser.
 */

#ifndef PACKAGELISTPARSER_H_
#define PACKAGELISTPARSER_H_

#include <stdbool.h>
#include <sys/types.h>

#ifdef __cplusplus
extern "C" {
#endif

/* The file containing the list of installed packages on the system */
#define PACKAGES_LIST_FILE  "/data/system/packages.list"

typedef struct pkg_info pkg_info;
typedef struct gid_list gid_list;

struct gid_list {
    size_t cnt;
    gid_t *gids;
};

struct pkg_info {
    char *name;
    uid_t uid;
    bool debuggable;
    char *data_dir;
    char *seinfo;
    gid_list gids;
    void *private_data;
};

typedef bool(*pfn_on_package)(pkg_info *info, void *userdata);

extern bool packagelist_parse(pfn_on_package callback, void *userdata);
extern void packagelist_free(pkg_info *info);

#ifdef __cplusplus
}
#endif

#endif /* PACKAGELISTPARSER_H_ */

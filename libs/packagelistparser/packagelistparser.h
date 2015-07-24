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

/** The file containing the list of installed packages on the system */
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

/**
 * Callback function to be used by packagelist_parse() routine.
 * @param info
 *  The parsed package information
 * @param userdata
 *  The supplied userdata pointer to packagelist_parse()
 * @return
 *  true to keep processing, false to stop.
 */
typedef bool(*pfn_on_package)(pkg_info *info, void *userdata);

/**
 * Parses the file specified by PACKAGES_LIST_FILE and invokes the callback on
 * each entry found. Once the callback is invoked, ownership of the pkg_info pointer
 * is passed to the callback routine, thus they are required to perform any cleanup
 * desired.
 * @param callback
 *  The callback function called on each parsed line of the packages list.
 * @param userdata
 *  An optional userdata supplied pointer to pass to the callback function.
 * @return
 *  true on success false on failure.
 */
extern bool packagelist_parse(pfn_on_package callback, void *userdata);

/**
 * Frees a pkg_info structure.
 * @param info
 *  The struct to free
 */
extern void packagelist_free(pkg_info *info);

#ifdef __cplusplus
}
#endif

#endif /* PACKAGELISTPARSER_H_ */

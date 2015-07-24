#include <errno.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <sys/limits.h>

#define LOG_TAG "packagelistparser"
#include <utils/Log.h>

#include "packagelistparser.h"

static inline bool get_gid_cnt(const char *gids, size_t *cnt)
{
    if (!strcmp(gids, "none")) {
        *cnt = 0;
        return true;
    }

    for (*cnt = 1; gids[*cnt]; gids[*cnt] == ',' ? (*cnt)++ : *gids++)
        ;

    return true;
}

static bool parse_gids(char *gids, gid_t *gid_list, size_t *cnt)
{
    gid_t gid;
    char* token;
    char *endptr;
    size_t cmp = 0;

    while ((token = strsep(&gids, ",\n"))) {

        if (cmp > *cnt) {
            return false;
        }

        gid = strtoul(token, &endptr, 10);
        if (*endptr != '\0') {
            return false;
        }

        gid_list[cmp] = gid;
        cmp++;
    }
    return true;
}

extern bool packagelist_parse(pfn_on_package callback, void *userdata)
{
    bool rc = false;
    char *buf = NULL;
    size_t buflen = 0;
    ssize_t bytesread;
    FILE *fp;
    char *cur;
    char *next;
    char *endptr;
    unsigned long tmp;

    unsigned long lineno = 1;
    const char *errmsg = NULL;
    struct pkg_info *pkg_info = NULL;

    fp = fopen(PACKAGES_LIST_FILE, "r");
    if (!fp) {
        ALOGE("Could not open: \"%s\", error(%d): %s\n", PACKAGES_LIST_FILE,
                errno, strerror(errno));
        return false;
    }

    while ((bytesread = getline(&buf, &buflen, fp)) > 0) {

        pkg_info = calloc(1, sizeof(*pkg_info));
        if (!pkg_info) {
            errmsg = "oom allocating \"pkg_info\"";
            goto err;
        }

        next = buf;

        cur = strsep(&next, " \t\n");
        if (!cur) {
            errmsg = "Could not get next token for \"package name\"";
            goto err;
        }

        pkg_info->name = strdup(cur);
        if (!pkg_info->name) {
            errmsg = "oom allocating \"package name\"";
            goto err;
        }

        cur = strsep(&next, " \t\n");
        if (!cur) {
            errmsg = "Could not get next token for \"uid\"";
            goto err;
        }

        tmp = strtoul(cur, &endptr, 10);
        if (*endptr != '\0') {
            errmsg = "Could not convert uid to integer value";
            goto err;
        }

        /*
         * if unsigned long is greater than size of uid_t,
         * prevent a truncation based roll-over
         */
        if (tmp > UID_MAX) {
            errmsg = "Field uid greater than UID_MAX";
            goto err;
        }

        pkg_info->uid = (uid_t) tmp;

        cur = strsep(&next, " \t\n");
        if (!cur) {
            errmsg = "Could not get next token for \"debuggable\"";
            goto err;
        }

        tmp = strtoul(cur, &endptr, 10);
        if (*endptr != '\0') {
            errmsg = "Could not convert debuggable to integer value";
            goto err;
        }

        /* should be a valid boolean of 1 or 0 */
        if (!(tmp == 0 || tmp == 1)) {
            errmsg = "Field debuggable is not 0 or 1 boolean value";
            goto err;
        }

        pkg_info->debuggable = (bool) tmp;

        cur = strsep(&next, " \t\n");
        if (!cur) {
            errmsg = "Could not get next token for \"data dir\"";
            goto err;
        }

        pkg_info->data_dir = strdup(cur);
        if (!pkg_info->data_dir) {
            errmsg = "oom allocating \"data dir\"";
            goto err;
        }

        cur = strsep(&next, " \t\n");
        if (!cur) {
            errmsg = "Could not get next token for \"seinfo\"";
            goto err;
        }

        pkg_info->seinfo = strdup(cur);
        if (!pkg_info->seinfo) {
            errmsg = "oom allocating \"seinfo\"";
            goto err;
        }

        cur = strsep(&next, " \t\n");
        if (!cur) {
            errmsg = "Could not get next token for \"gid(s)\"";
            goto err;
        }

        /*
         * Parse the gid list, could be in the form of none, single gid or list:
         * none
         * gid
         * gid, gid ...
         */
        rc = get_gid_cnt(cur, &pkg_info->gids.cnt);
        if (!rc) {
            errmsg = "Could not parse gid list to obtain count";
            goto err;
        }

        if (pkg_info->gids.cnt > 0) {

            pkg_info->gids.gids = calloc(pkg_info->gids.cnt, sizeof(gid_t));
            if (!pkg_info->gids.gids) {
                errmsg = "oom allocating \"gid list\"";
                goto err;
            }

            rc = parse_gids(cur, pkg_info->gids.gids, &pkg_info->gids.cnt);
            if (!rc) {
                errmsg = "Could not parse gid list";
                goto err;
            }
        }

        rc = callback(pkg_info, userdata);
        if (false) {
            /*
             * We do not log this as this can be intentional from
             * callback to abort processing. We go to out to not
             * free the pkg_info
             */
            rc = true;
            goto out;
        }
        lineno++;
    }

    rc = true;

out:
    free(buf);
    fclose(fp);
    return rc;

err:
    ALOGE("Error Parsing \"%s\" on line: %lu for reason: %s",
            PACKAGES_LIST_FILE, lineno, errmsg);
    rc = false;
    packagelist_free(pkg_info);
    goto out;
}

void packagelist_free(pkg_info *info)
{
    if (info) {
        free(info->name);
        free(info->data_dir);
        free(info->seinfo);
        free(info->gids.gids);
        free(info);
    }
}

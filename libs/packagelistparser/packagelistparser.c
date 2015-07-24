#include <errno.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define LOG_TAG "packagelistparser"
#include <utils/Log.h>

#include "packagelistparser.h"

static inline bool get_gid_cnt(const char *gids, size_t *cnt) {

    if(gids == NULL || cnt == NULL) {
        return false;

    }

    if (!strcmp(gids, "none")) {
        *cnt = 0;
        return true;
    }

    for (*cnt=1; gids[*cnt]; gids[*cnt]==',' ? (*cnt)++ : *gids++)
    ;

    return true;
}

static bool parse_gids(char *gids, gid_t *gid_list, size_t *cnt) {

    gid_t gid;
    char* token;
    char *endptr;
    char *saveptr;

    size_t cmp = 0;

    token = strtok_r(gids, ",", &saveptr);
    while (token != NULL) {

        if (cmp > *cnt) {
            return false;
        }

        gid = strtoul(token, &endptr, 10);
        if(*endptr != '\0') {
            return false;
        }

        /* gid should never be root */
        if (gid == 0) {
            return false;
        }

           gid_list[cmp] = gid;

           cmp ++;
        token = strtok_r(NULL, ",", &saveptr);
    }
    return true;
}

extern bool packagelist_parse(pfn_on_package callback, void *userdata) {

    bool rc = true;
    char *buf = NULL;
    size_t buflen = 0;
    ssize_t bytesread;
    FILE *fp;
    char *cur;
    char *next;
    char *endptr;
    struct pkg_info *pkg_info = NULL;
    unsigned long lineno = 1;

    if (!callback) {
        ALOGE("No callback function registered");
        return false;
    }

    fp = fopen(PACKAGES_LIST_FILE, "r");
    if (!fp) {
        ALOGE("Could not open: \"%s\", error(%d): %s\n", PACKAGES_LIST_FILE, errno,
            strerror(errno));
        return false;
    }
    while ((bytesread = getline(&buf, &buflen, fp)) > 0) {

        pkg_info = calloc(1, sizeof(*pkg_info));

        if (!pkg_info) {
            goto err;
        }

        next = buf;

        cur = strsep(&next, " \t\n");
        if (!cur) {
            goto err;
        }

        pkg_info->name = strdup(cur);
        if (!pkg_info->name) {
            goto err;
        }

        cur = strsep(&next, " \t\n");
        if (!cur) {
            goto err;
        }

        pkg_info->uid = (uid_t)strtoul(cur, &endptr, 10);
        if(*endptr != '\0') {
            goto err;
        }

        /* There should never be a root app */
        if (!pkg_info->uid) {
            goto err;
        }

        cur = strsep(&next, " \t\n");
        if (!cur) {
            goto err;
        }

        pkg_info->debuggable = (bool)strtoul(cur, &endptr, 10);
        if(*endptr != '\0') {
            goto err;
        }

        cur = strsep(&next, " \t\n");
        if (!cur) {
            goto err;
        }

        pkg_info->data_dir = strdup(cur);
        if (!pkg_info->data_dir) {
            goto err;
        }

        cur = strsep(&next, " \t\n");
        if (!cur) {
            goto err;
        }

        pkg_info->seinfo = strdup(cur);
        if (!pkg_info->seinfo) {
            goto err;
        }

        cur = strsep(&next, " \t\n");
        if (!cur) {
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
            ALOGE("Could not parse gid list to obtain count");
            goto err;
        }

        if (pkg_info->gids.cnt > 0) {

            pkg_info->gids.gids = calloc(pkg_info->gids.cnt, sizeof(gid_t));
            rc = parse_gids(cur, pkg_info->gids.gids, &pkg_info->gids.cnt);
            if (!rc) {
                ALOGE("Could not parse gid list");
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
            goto out;
        }

        lineno++;
    }

out:
    free(buf);
    fclose(fp);
    return rc;

err:
    ALOGE("Error Parsing");
    rc = false;
    packagelist_free(pkg_info);
    goto out;
}

void packagelist_free(pkg_info *info) {

    if (info) {
        free(info->name);
        free(info->data_dir);
        free(info->seinfo);
        free(info->gids.gids);
        free(info);
    }
}

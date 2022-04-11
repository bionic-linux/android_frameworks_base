#!/usr/bin/python
# Usage: python $ANDROID_BUILD_TOP/frameworks/base/tools/owners/experts.py | sort

import os, re, collections

# Discover active team members via:
# ganpati2 --ganpati2=ganpati2  membership list --parent %android-api-council.prod > /tmp/api_ldaps
# ganpati2 --ganpati2=ganpati2  membership list --parent %android-api-council-elders.prod >> /tmp/api_ldaps
with open("/tmp/api_ldaps") as f:
    COUNCIL_MEMBERS = f.read().split("\n")


def find_packages(path):
    if os.path.exists(path):
        with open(path) as f:
            return re.findall("package ([^ ]+?) ", f.read())
    else:
        return []

def find_owners(path):
    res = set()
    if os.path.exists(path):
        cwd = os.path.dirname(path)
        with open(path) as f:
            for l in f.readlines():
                l = l.strip()
                if l.startswith("#"): continue
                if l.startswith("per-file"): continue
                if l.startswith("include "):
                    res.update(find_owners_include(cwd, l[len("include "):]))
                if l.startswith("file:"):
                    res.update(find_owners_include(cwd, l[len("file:"):]))
                elif "@" in l and "LAST_RESORT_SUGGESTION" not in l:
                    res.add(l[0:l.index("@")])
    return res

def find_owners_include(cwd, path):
    if "#" in path:
        path = path[0:path.index("#")].strip()
    path = path.split(":")
    path = [ p.strip("/") for p in path ]
    owners = set()
    if len(path) == 1:
        owners.update(find_owners(os.path.join(cwd, path[0])))
        path = [ "platform/frameworks/base", path[0] ]
    if len(path) == 3:
        path = [ path[0], path[2] ]
    path[0] = path[0].replace("platform/", "")
    owners.update(find_owners(os.path.join(os.environ.get("ANDROID_BUILD_TOP"), path[0], path[1])))
    return owners


# find all directories that might be defining APIs
srcs = set()
for root, dirs, files in os.walk("."):
    if "api" in dirs:
        srcs.add(root)

# figure out the packages contained and relevant OWNERS
res = collections.defaultdict(lambda: set())
for src in srcs:
    pkgs = set()
    pkgs.update(find_packages(os.path.join(src, "api/current.txt")))
    pkgs.update(find_packages(os.path.join(src, "api/system-current.txt")))

    for pkg in pkgs:
        pkg = pkg.replace(".", "/")
        owners = set()
        owners.update(find_owners(os.path.join(src, "java", pkg, "OWNERS")))
        owners.update(find_owners(os.path.join(src, "src", pkg, "OWNERS")))

        # if nothing specific found, we're willing to walk up a few parents
        if len(owners) == 0:
            owners.update(find_owners(os.path.join(src, "OWNERS")))
        if len(owners) == 0:
            owners.update(find_owners(os.path.join(src, "..", "OWNERS")))

        owners = [ "%s@" % (o) for o in sorted(owners) if o in COUNCIL_MEMBERS ]
        if len(owners) == 0: continue

        pkg = pkg.replace("/", ".")
        res[pkg].update(owners)

# try collapsing identical sub-packages
for pkg in list(res.keys()):
    owners = res[pkg]
    probe = pkg.split(".")
    while len(probe) > 1:
        del probe[-1]
        probe_pkg = ".".join(probe)
        if probe_pkg in res:
            if res[probe_pkg] == owners:
                del res[pkg]
            break

for pkg in list(res.keys()):
    owners = res[pkg]
    print("%s.*\t%s" % (pkg, "\t".join(owners)))

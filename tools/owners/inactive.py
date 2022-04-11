#!/usr/bin/python

# Usage:
# ganpati2 membership list --parent %android-team-auto.prod,%android-access.prod > /tmp/active_ldaps
# inactive.py /tmp/active_ldaps

import sys, os, re, collections

def filter_inactive(raw, active_ldaps):
    res = []
    for line in raw.split("\n"):
        if not line.startswith("#") and "@" in line:
            refs = re.findall(r"([a-z\-+]+@(?:android.com|google.com))", line)
            for ref in refs:
                ldap = ref[0:ref.index("@")]
                if ldap not in active_ldaps:
                    line = re.sub(r"%s,? ?" % (ref), "", line)
            if line.startswith("per-file "):
                if line.endswith(","):
                    line = line[0:-1]
                elif line.endswith(", "):
                    line = line[0:-2]
            if "@" not in line:
                line = None
        if line is not None:
            res.append(line)
    return "\n".join(res)


def main():
    with open(sys.argv[1]) as f:
        active_ldaps = re.findall("INCLUDE ([a-z]+)", f.read())

    for root, dirs, files in os.walk("."):
        for name in files:
            name = os.path.join(root, name)
            if "OWNER" in name:
                with open(name, "r+") as f:
                    before = f.read()
                    after = filter_inactive(before, active_ldaps)
                    if before != after:
                        f.seek(0)
                        f.write(after)
                        f.truncate()

if __name__ == "__main__":
    main()

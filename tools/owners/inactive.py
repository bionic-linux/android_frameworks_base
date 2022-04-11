#!/usr/bin/python
# Usage: python $ANDROID_BUILD_TOP/frameworks/base/tools/owners/inactive.py

import os, re, collections

# Discover active team members via:
# ganpati2 --ganpati2=ganpati2  membership list --parent %android-team-auto.prod > /tmp/active_ldaps
# ganpati2 --ganpati2=ganpati2  membership list --parent %android-access.prod >> /tmp/active_ldaps
with open("/tmp/active_ldaps") as f:
    ACTIVE_LDAPS = f.read().split("\n")

for root, dirs, files in os.walk("."):
    for name in files:
        name = os.path.join(root, name)
        if "OWNER" in name:
            with open(name, "r+") as f:
                raw = f.read()
                before = raw
                refs = re.findall(r"([a-z\-+]+@(?:android.com|google.com))", raw)
                for ref in refs:
                    ldap = ref[0:ref.index("@")]
                    if ldap not in ACTIVE_LDAPS:
                        raw = re.sub(r", ?%s" % (ref), "", raw)
                        raw = re.sub(r"%s, ?" % (ref), "", raw)
                        raw = re.sub(r"%s\n?" % (ref), "", raw)
                if raw != before:
                    f.seek(0)
                    f.write(raw)
                    f.truncate()

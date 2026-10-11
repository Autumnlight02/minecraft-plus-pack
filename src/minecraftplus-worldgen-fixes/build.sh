#!/bin/sh
# Rebuilds the jar into ../../mods/. Run from anywhere.
set -e
cd "$(dirname "$0")"
OUT=../../mods/minecraftplus-worldgen-fixes-1.0.0.jar
rm -f "$OUT"
python3 - "$OUT" <<'PY'
import os, sys, zipfile
out = sys.argv[1]
with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as z:
    for root, _, files in sorted(os.walk('.')):
        for f in sorted(files):
            p = os.path.join(root, f)[2:]
            if p in ('build.sh', 'README.txt'):
                continue
            zi = zipfile.ZipInfo(p, date_time=(1980, 2, 1, 0, 0, 0))
            zi.compress_type = zipfile.ZIP_DEFLATED
            z.writestr(zi, open(p, 'rb').read())
print('built', out)
PY

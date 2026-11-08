#!/bin/sh
# Compiles the mod and rebuilds the jar into ../../mods/ (or into $1). Run from anywhere.
# Needs: the local dedicated server ($HOME/mcplus-server): its JDK 21, libraries, and the Create, Advanced Peripherals
# and Curios API jars in mods/ (compile-only, not bundled).
set -e
cd "$(dirname "$0")"
S=$(readlink -f "$HOME/mcplus-server")
CP=$(find "$S/libraries" -name '*.jar' | tr '\n' ':')
CP="$CP$(ls "$S"/mods/create-1.21.1-*.jar):$(ls "$S"/mods/AdvancedPeripherals-1.21.1-*.jar):$(ls "$S"/mods/curios-neoforge-*.jar)"
rm -rf build && mkdir -p build/classes
"$S/jdk/bin/javac" --release 21 -proc:none -nowarn -cp "$CP" -d build/classes $(find java -name '*.java')
OUT=${1:-../../mods/minecraftplus-glasses-goggles-1.0.0.jar}
rm -f "$OUT"
python3 - "$OUT" <<'PY'
import os, sys, zipfile
out = sys.argv[1]
files = [('META-INF/neoforge.mods.toml', 'META-INF/neoforge.mods.toml')]
for base in ('assets', 'data', 'build/classes'):
    for root, _, fs in os.walk(base):
        for f in fs:
            p = os.path.join(root, f)
            files.append((p, os.path.relpath(p, 'build/classes') if base == 'build/classes' else p))
with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as z:
    for src, name in sorted(files, key=lambda x: x[1]):
        zi = zipfile.ZipInfo(name, date_time=(1980, 2, 1, 0, 0, 0))
        zi.compress_type = zipfile.ZIP_DEFLATED
        z.writestr(zi, open(src, 'rb').read())
print('built', out)
PY
rm -rf build

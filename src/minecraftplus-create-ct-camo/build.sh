#!/bin/sh
# Compiles the mod and rebuilds the jar into ../../mods/ (or into $1). Run from anywhere.
# Needs: the local dedicated server ($HOME/mcplus-server): its JDK 21, libraries (Minecraft, NeoForge, sponge-mixin)
# and the Create and FramedBlocks jars in mods/ (compile-only, not bundled).
set -e
cd "$(dirname "$0")"
S=$(readlink -f "$HOME/mcplus-server")
CP=$(find "$S/libraries" -name '*.jar' | tr '\n' ':')
CP="$CP$(ls "$S"/mods/create-1.21.1-*.jar):$(ls "$S"/mods/FramedBlocks-*.jar)"
rm -rf build && mkdir -p build/classes
"$S/jdk/bin/javac" --release 21 -proc:none -Xlint:all -cp "$CP" -d build/classes $(find java -name '*.java')
OUT=${1:-../../mods/minecraftplus-create-ct-camo-1.0.0.jar}
rm -f "$OUT"
python3 - "$OUT" <<'PY'
import os, sys, zipfile
out = sys.argv[1]
files = [('META-INF/neoforge.mods.toml', 'META-INF/neoforge.mods.toml'),
         ('mcplus_create_ct_camo.mixins.json', 'mcplus_create_ct_camo.mixins.json')]
for root, _, fs in os.walk('build/classes'):
    for f in fs:
        p = os.path.join(root, f)
        files.append((p, os.path.relpath(p, 'build/classes')))
with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as z:
    for src, name in sorted(files, key=lambda x: x[1]):
        zi = zipfile.ZipInfo(name, date_time=(1980, 2, 1, 0, 0, 0))
        zi.compress_type = zipfile.ZIP_DEFLATED
        z.writestr(zi, open(src, 'rb').read())
print('built', out)
PY
rm -rf build

#!/bin/sh
# Compiles the fix and rebuilds the jar into ../../../server-mods/. Run from anywhere. SERVER ONLY (not in the client pack).
# Needs: JDK 21 javac (default = Prism's bundled one) and sponge-mixin + FML jars from ../../../server-full.
# stubs/ holds empty compile-only copies of three client classes; they are not packaged.
set -e
cd "$(dirname "$0")"
JAVAC=${JAVAC:-$(ls "$HOME"/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/java/java-runtime-delta/bin/javac 2>/dev/null | head -1)}
[ -x "$JAVAC" ] || JAVAC=javac
S=../../../server-full/libraries
CP="$(ls $S/net/fabricmc/sponge-mixin/*/sponge-mixin-*.jar | head -1):$(ls $S/net/neoforged/fancymodloader/loader/*/loader-*.jar | head -1)"
CP="$CP:$(ls $S/net/neoforged/mergetool/*/mergetool-*-api.jar | head -1)"
rm -rf build && mkdir -p build/classes build/stubs
"$JAVAC" --release 21 -proc:none -nowarn -d build/stubs $(find stubs -name '*.java')
"$JAVAC" --release 21 -proc:none -nowarn -cp "$CP:build/stubs" -d build/classes $(find java -name '*.java')
mkdir -p ../../../server-mods
OUT=../../../server-mods/minecraftplus-littletiles-measure-fix-1.0.0.jar
rm -f "$OUT"
python3 - "$OUT" <<'PY'
import os, sys, zipfile
out = sys.argv[1]
files = [('META-INF/neoforge.mods.toml', 'META-INF/neoforge.mods.toml'),
         ('minecraftplus_ltmeasurefix.mixins.json', 'minecraftplus_ltmeasurefix.mixins.json')]
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

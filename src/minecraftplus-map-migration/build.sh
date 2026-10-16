#!/bin/sh
# Compiles MapMigration and rebuilds the jar into ../../mods/. Run from anywhere.
# Needs: JDK 21 javac (default = Prism's bundled one) and FML's loader jar from the local server (server-full/).
set -e
cd "$(dirname "$0")"
JAVAC=${JAVAC:-$(ls "$HOME"/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/java/java-runtime-delta/bin/javac \
  "$HOME/Library/Application Support/PrismLauncher/java"/jdk-21*/Contents/Home/bin/javac 2>/dev/null | head -1)}
[ -x "$JAVAC" ] || JAVAC=javac
CP=$(ls ../../../server-full/libraries/net/neoforged/fancymodloader/loader/*/loader-*.jar | head -1)
rm -rf build && mkdir -p build/classes
"$JAVAC" --release 21 -proc:none -nowarn -cp "$CP" -d build/classes $(find java -name '*.java')
OUT=../../mods/minecraftplus-map-migration-1.1.0.jar
rm -f "$OUT"
python3 - "$OUT" <<'PY'
import os, sys, zipfile
out = sys.argv[1]
files = [('META-INF/neoforge.mods.toml', 'META-INF/neoforge.mods.toml')]
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

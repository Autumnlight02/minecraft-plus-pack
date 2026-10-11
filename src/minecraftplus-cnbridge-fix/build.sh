#!/bin/sh
# Compiles the two mixins and rebuilds the jar into ../../mods/. Run from anywhere.
# Needs: JDK 21 (javac; default = Prism's bundled one) and the jars of the local dedicated server (server-full/:
# Minecraft, sponge-mixin, sable, cnbridge) for the compile classpath.
set -e
cd "$(dirname "$0")"
JAVAC=${JAVAC:-$(ls -d "$HOME/Library/Application Support/PrismLauncher/java"/jdk-21*/Contents/Home/bin/javac 2>/dev/null | head -1)}
[ -x "$JAVAC" ] || JAVAC=javac
S=../../../server-full
CP="$S/libraries/net/minecraft/server/1.21.1-20240808.144430/server-1.21.1-20240808.144430-srg.jar"
CP="$CP:$(ls $S/libraries/net/fabricmc/sponge-mixin/*/sponge-mixin-*.jar | head -1)"
CP="$CP:$(ls $S/mods/sable-neoforge-*.jar | head -1):$S/mods/cnbridge-1.0.0.jar"
CP="$CP:$(ls $S/libraries/org/joml/joml/*/joml-*.jar | head -1)"
rm -rf build && mkdir -p build/classes
# Sable's API types (SableCompanion) live in a jar nested inside the Sable jar
unzip -q -o -j "$(ls $S/mods/sable-neoforge-*.jar | head -1)" 'META-INF/jarjar/sable-companion-common-*.jar' -d build
CP="$CP:$(ls build/sable-companion-common-*.jar | head -1)"
"$JAVAC" --release 21 -proc:none -nowarn -cp "$CP" -d build/classes $(find java -name '*.java')
OUT=../../mods/minecraftplus-cnbridge-fix-1.0.0.jar
rm -f "$OUT"
python3 - "$OUT" <<'PY'
import os, sys, zipfile
out = sys.argv[1]
files = [('META-INF/neoforge.mods.toml', 'META-INF/neoforge.mods.toml'),
         ('minecraftplus_cnbridgefix.mixins.json', 'minecraftplus_cnbridgefix.mixins.json')]
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

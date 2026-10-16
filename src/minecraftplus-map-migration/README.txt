Minecraft Plus: Map Migration (minecraftplus_mapmigration), client-side, 2026-10-16

Problem: Xaero's minimap and world map store waypoints and map tiles per server ADDRESS
(xaero/minimap/Multiplayer_<host>, xaero/world-map/Multiplayer_<host>). When the home IP changed
(78.42.137.21 -> 37.201.224.126 on 2026-10-15), friends got an empty map and lost their waypoints.

Fix: at game start (mod constructor, before any world is joined) it merges old -> new for each rule in
config/minecraftplus-address-migration.txt, once per rule (done list: xaero/minecraftplus-migrated.txt):
  - files only in the old folder are copied; old folders are never changed
  - waypoint files: old waypoints the new file lacks (same name + x/y/z) are appended
  - world map regions (*.zip) in both: merged chunk by chunk (XaeroRegion.java, Xaero save format 6.8): every explored
    chunk of both is kept; where both have the same chunk, the new address's (newer) one wins. The merged file must
    read back before it replaces the new one; on any error the new region stays untouched. Texture cache is cleared.
  - other files in both (configs, caches): the new folder's copy is kept
Installs that have map data for the LAN address 192.168.178.132 (home network) are skipped.
On a dedicated server it does nothing (no xaero/ folder).

Next address change: add a line "old -> new" to config/minecraftplus-address-migration.txt; no rebuild needed.
Build: ./build.sh (javac from Prism's JDK 21; classpath: FML loader jar from ../../../server-full), then update index.

1.1.0 (2026-10-16, pack 0.4.7): chunk-level region merge. 1.0.0 (pack 0.4.4) replaced a whole region with the
bigger file, which dropped chunks explored only under the new address in that region. 1.1.0 uses a new done-key
("v2 old -> new"), so it runs once more on installs where 1.0.0 already ran and merges the remaining old chunks in.
Verified: all 186 region files of Autumn's instance decode and re-encode byte-identical (50,947 tiles); merges of
overlapping regions read back with every tile from the right source.

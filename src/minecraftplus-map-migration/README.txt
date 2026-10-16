Minecraft Plus: Map Migration (minecraftplus_mapmigration), client-side, 2026-10-16

Problem: Xaero's minimap and world map store waypoints and map tiles per server ADDRESS
(xaero/minimap/Multiplayer_<host>, xaero/world-map/Multiplayer_<host>). When the home IP changed
(78.42.137.21 -> 37.201.224.126 on 2026-10-15), friends got an empty map and lost their waypoints.

Fix: at game start (mod constructor, before any world is joined) it merges old -> new for each rule in
config/minecraftplus-address-migration.txt, once per rule (done list: xaero/minecraftplus-migrated.txt):
  - files only in the old folder are copied; old folders are never changed
  - waypoint files: old waypoints the new file lacks (same name + x/y/z) are appended
  - world map regions (*.zip) in both: the bigger file wins; that map's texture cache is cleared (rebuilt)
  - other files in both (configs, caches): the new folder's copy is kept
Installs that have map data for the LAN address 192.168.178.132 (home network) are skipped.
On a dedicated server it does nothing (no xaero/ folder).

Next address change: add a line "old -> new" to config/minecraftplus-address-migration.txt; no rebuild needed.
Build: ./build.sh (javac from Prism's JDK 21; classpath: FML loader jar from ../../../server-full), then update index.

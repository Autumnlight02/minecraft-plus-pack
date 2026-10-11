Minecraft Plus: Dimensional Drive Fix (minecraftplus_cnbridgefix)

Problem (found 2026-10-07, G2): cnbridge 1.0.0 (Modrinth "Create: Northstar-Aeronautics Compatibility" 3.0.0)
finds "the ship this drive is on" by returning the FIRST Sable sub-level in the drive's dimension. With more than
one ship in a dimension, the drive checks the wrong ship's altitude (never fires) or, worse, transfers the wrong ship.

Fix: two mixins replace DimensionalDriveBlockEntity.findSubLevel and DebugDimensionalDriveBlockEntity.findSubLevel
with Sable.HELPER.getContaining(level, pos), i.e. the ship whose plot contains the drive block.

Build: ./build.sh (javac from Prism's JDK 21; classpath from ../../../server-full), then `packwiz refresh`.
Remove this mod when cnbridge fixes findSubLevel upstream.

Minecraft Plus: Grappling Hook on Ships (minecraftplus_grapplefix 1.0.0)

Bug: blocks of a Sable ship really live in the ship's "plot", millions of blocks away, and Sable reports a hit on
such a block in plot coordinates. Grappling Hook Mod v13 (GrapplehookEntity.serverAttach) used that hit position as
the hook's anchor: the hook jumped to e.g. 20,481,037 / 129 / 20,499,468, the rope pointed into the distance and the
pull went the wrong way (2026-10-07, Autumn's test; notes/G6/human/).

Fix (one mixin, GrapplehookOnShipMixin):
- serverAttach HEAD: if the hit block is in a Sable sub-level, keep the plot position and replace the anchor with
  the ship's visible position (logicalPose().transformPosition).
- tick HEAD (server): while attached, move the anchor with the ship and send the mod's own GrappleAttachPos packet
  to nearby players, so the client-side rope physics pulls toward the moving ship.

Build: ./build.sh (javac from Prism's JDK; classpath from ../../../server-full jars). Output:
pack/mods/minecraftplus-grapple-fix-1.0.0.jar. Then `packwiz refresh` and copy the jar to server-full/mods.

SUPERSEDED 2026-10-07: the pack now uses Grappling Hook Mod: Skybound (fork with Create contraption + Sable
support); this mod is no longer built into pack/mods. Source kept for reference.

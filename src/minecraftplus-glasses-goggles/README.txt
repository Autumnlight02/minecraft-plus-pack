Minecraft Plus: Goggles Module (mcplus_glasses_goggles), client + server, 2026-11-07

Item mcplus_glasses_goggles:goggles_module, an Advanced Peripherals Smart Glasses module (IModuleItem; the module
itself does nothing in CC, no tick work). Recipe: shapeless create:goggles + advancedperipherals:peripheral_casing.

Smart Glasses with the module installed count as Create Engineer's Goggles (goggle overlay, rotation indicator,
kinetic stats in tooltips). The mod adds a predicate to GogglesItem.addIsWearingPredicate in its constructor, like
Create's own Curios compat (com.simibubi.create.compat.curios.Curios.init). It checks:
  - the helmet slot
  - the Curios "head" slot (the Accessories compat layer maps it to the Accessories "hat" slot)
for a SmartGlassesItem whose advancedperipherals:items component holds a Goggles Module. Other curios slots are
not checked: AP tags the glasses only for curios:head.

Build: ./build.sh [out.jar] (default ../../mods/minecraftplus-glasses-goggles-1.0.0.jar). Compiles with the JDK and
libraries of $HOME/mcplus-server against its Create, Advanced Peripherals and Curios API jars (not bundled).
The server's Minecraft jar is unpatched, so NeoForge-patched methods (Entity#getCapability) and access-transformed
fields don't compile; use the NeoForge API equivalents (EntityCapability#getCapability).

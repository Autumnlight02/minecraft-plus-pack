Minecraft Plus: Data Fixes (code-free NeoForge mod, modId minecraftplus_datafixes)
Build: ./build.sh  ->  pack/mods/minecraftplus-data-fixes-1.0.0.jar  (deterministic zip)

What it overrides (all optional deps with ordering AFTER, so this pack wins):
- data/createdeco/recipe/placard.json: Create Deco 2.1.3 writes the white-dye ingredient as {"id": ...},
  which 1.21.1 ingredients don't accept; same recipe with {"item": ...}.
- data/create/recipe/{smelting,blasting}/silver_ingot_compat_galosphere.json: Create's compat recipe outputs
  galosphere:silver_ingot, which Galosphere 1.21 no longer has (silver became palladium). Original recipe plus a
  neoforge:item_exists condition, so it loads only if that item ever exists again.
- data/dndecor/loot_table/blocks/*.json (256) and data/create_connected/loot_table/blocks/*.json (16): loot tables
  for Dye Depot colour variants that are only registered when Dye Depot is installed (it isn't). Original tables
  plus neoforge:item_exists conditions: silent now, still correct if Dye Depot is added.
- data/copycats/physics_block_properties/copycat_{step,catwalk}.json: Sable mass entries (from Copycats+ and
  Copycats+ Aeronautics Weight) for blocks Copycats+ 3.0.9 does not register. Pointed at the empty block tag
  #minecraftplus_datafixes:none so Sable has nothing to apply and stops logging errors.
- data/cnbridge/recipe/dimensional_drive.json: cnbridge 3.0.0's drive recipe needs northstar:computer_chip, which
  Northstar 0.6.x removed. Same shape, chip replaced by northstar:targeting_computer.
Re-check every file when one of those mods updates.

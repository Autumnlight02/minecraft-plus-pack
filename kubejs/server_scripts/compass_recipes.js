// Autumn's note (GOAL.md C2): both compasses should cost a real journey.
// Nature's Compass: saplings from eight different biomes, so you must travel before you can unlock it.
// Explorer's Compass: loot that only drops in structures (sherds from archaeology, trial key, echo shards,
// name tags), so you must explore structures first.
ServerEvents.recipes(event => {
  event.remove({ id: 'naturescompass:natures_compass' })
  event.shaped('naturescompass:naturescompass', [
    'ABC',
    'DXE',
    'FGH'
  ], {
    A: 'minecraft:jungle_sapling',     // jungle
    B: 'minecraft:acacia_sapling',     // savanna
    C: 'minecraft:cherry_sapling',     // cherry grove
    D: 'minecraft:spruce_sapling',     // taiga
    E: 'minecraft:mangrove_propagule', // mangrove swamp
    F: 'minecraft:dark_oak_sapling',   // dark forest
    G: 'minecraft:birch_sapling',      // birch forest
    H: 'minecraft:oak_sapling',
    X: 'minecraft:compass'
  }).id('naturescompass:natures_compass')

  event.remove({ id: 'explorerscompass:explorers_compass' })
  event.shaped('explorerscompass:explorerscompass', [
    'SKS',
    'EXE',
    'SNS'
  ], {
    S: '#minecraft:decorated_pot_sherds', // archaeology at structures
    K: 'minecraft:trial_key',             // trial chambers
    E: 'minecraft:echo_shard',            // ancient cities
    N: 'minecraft:name_tag',              // structure chests (and fishing/trades)
    X: 'minecraft:compass'
  }).id('explorerscompass:explorers_compass')
})

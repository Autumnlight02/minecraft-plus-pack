// Autumn (2026-10-18): Advanced Peripherals ships the Smart Chestplate without a recipe. Its 5 hands mine and fight
// within 10 blocks with no fuel or cooldown, so it costs endgame items: a Nether Star (in the overpowered core),
// a Warden kill (Heart of the Deep), netherite and a Create mechanical crafter setup.
ServerEvents.recipes(event => {
  event.recipes.create.mechanical_crafting('advancedperipherals:smart_chestplate', [
    ' I I ',
    'DPOPD',
    'D N D',
    'PIHIP',
    '  D  '
  ], {
    O: 'advancedperipherals:overpowered_weak_automata_core',
    N: 'minecraft:netherite_chestplate',
    H: 'deeperdarker:heart_of_the_deep',
    D: 'create:deployer',
    P: 'create:precision_mechanism',
    I: 'minecraft:netherite_ingot'
  }).id('minecraftplus:smart_chestplate')
})

// Autumn (2026-11-06): a crafted Elytra, gated behind a Nether Star and a Paraglider.
ServerEvents.recipes(event => {
  event.shaped('minecraft:elytra', [
    'MPM',
    'FNF',
    'FSF'
  ], {
    M: 'minecraft:phantom_membrane',
    P: 'paraglider:paraglider',
    F: 'minecraft:feather',
    N: 'minecraft:nether_star',
    S: 'minecraft:string'
  }).id('minecraftplus:elytra')
})

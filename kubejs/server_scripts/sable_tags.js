// Sable Physics Compat 1.3.0 ships data/sable/tags/block/bouncy.json with a trailing comma, so the whole file
// fails to load. Of its entries only Alex's Caves sundrop is in this pack; add it back here.
ServerEvents.tags('block', event => {
  event.add('sable:bouncy', 'alexscaves:sundrop')
})

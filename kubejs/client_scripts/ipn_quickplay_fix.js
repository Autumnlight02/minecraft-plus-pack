// Inventory Profiles Next 2.2.5 has a static-init cycle (Features <-> ConfigScreenSettings). Normally the title
// screen initialises ConfigScreenSettings first. When a world or server is opened straight from launch (Prism
// "--world"/"--server" quick play) Features goes first and the game crashes with an NPE on
// Features.ENABLE_PROFILES$delegate. Touching ConfigScreenSettings here restores the normal order.
try {
  Java.loadClass('org.anti_ad.mc.ipnext.config.ConfigScreenSettings').INSTANCE
} catch (e) {
  console.warn('IPN quick-play fix skipped: ' + e)
}

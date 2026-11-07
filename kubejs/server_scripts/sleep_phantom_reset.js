// Vanilla resets the phantom timer only for players who were in bed. A night skip with playersSleepingPercentage
// should count for everyone online. An exception in a native listener can abort the event, hence the try.
const SPR_Stats = Java.loadClass('net.minecraft.stats.Stats')
const SPR_SleepEvent = Java.loadClass('net.neoforged.neoforge.event.level.SleepFinishedTimeEvent')
const SPR_TimeSinceRest = SPR_Stats.CUSTOM.get(SPR_Stats.TIME_SINCE_REST)

NativeEvents.onEvent(SPR_SleepEvent, event => {
  try {
    event.level.server.players.forEach(p => p.resetStat(SPR_TimeSinceRest))
  } catch (e) {
    console.error('sleep_phantom_reset: ' + e)
  }
})

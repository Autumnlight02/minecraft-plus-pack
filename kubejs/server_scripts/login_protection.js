// Login protection: players who die while their client is still loading (MrTwister, bear, 2026-10-25).
// From login until 6 s after the client has loaded, an attack, a broken or placed block, a right click, or 80 s if it never loads:
// no damage, mobs can't target them, and air, fire, freezing, hunger and fall distance stay at their login values.
// Gravity is off until the first move or the end, so a player who logged out in the air hangs there and then gets their speed back.
// Nothing tells the server that a client has loaded. The client sends one position at login and then, while it runs, one at
// least every second, so two packets close together mean it runs. A packet overwrites known movement, which shows it arrived.
// The client owns player speed: only getKnownMovement has the real value, and hurtMarked sends the new one to the client.
// PlayerEvents.tick runs inside ServerPlayer.doTick, where the server simulates physics and then moves the player back.
// Position is only real after that, so all checks run in ServerEvents.tick. Restoring fall distance killed a loading player.
// KubeJS hides some vanilla methods (getUUID, level(), getGameTime, DamageSource.getEntity) and exposes uuid, level,
// x, source.actual instead. All server scripts share one scope, so every top-level name has the LP_ prefix.
// An exception in a native listener aborts the damage for everyone (2026-10-25: 2 min without any damage), so each
// listener returns at once while nobody is protected and any error switches the whole script off.
// Create Deployers and other fake players carry their owner's UUID, so a UUID match alone is not the player.
// Rhino fails with "redeclaration" on const/let inside a try block: declare them in the helper functions.
const LP_DamageEvent = Java.loadClass('net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent')
const LP_TargetEvent = Java.loadClass('net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent')
const LP_Mob = Java.loadClass('net.minecraft.world.entity.Mob')
const LP_MemoryModuleType = Java.loadClass('net.minecraft.world.entity.ai.memory.MemoryModuleType')
const LP_DamageTypeTags = Java.loadClass('net.minecraft.tags.DamageTypeTags')
const LP_FakePlayer = Java.loadClass('net.neoforged.neoforge.common.util.FakePlayer')
const LP_UNSEEN = new (Java.loadClass('net.minecraft.world.phys.Vec3'))(1e-7, 0, 0)
const LP_TitleText = Java.loadClass('net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket')
const LP_TitleTimes = Java.loadClass('net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket')
const LP_TitleClear = Java.loadClass('net.minecraft.network.protocol.game.ClientboundClearTitlesPacket')

const LP_MAX_TICKS = 1600
const LP_GRACE_TICKS = 120
const LP_FIRST_MOVE_SQ = 0.0025
const LP_LOGIN_PACKET_TICKS = 40
const LP_MAX_PACKET_GAP_TICKS = 22
let LP_players = {}
let LP_leftInAir = {}
let LP_active = 0
let LP_failed = false

function LP_fail(e) {
  LP_failed = true
  Object.keys(LP_players).forEach(id => LP_players[id].player.setNoGravity(false))
  LP_players = {}
  LP_leftInAir = {}
  LP_active = 0
  console.error(`login_protection disabled after an error: ${e}`)
}

function LP_isProtected(entity) {
  return entity != null && !(entity instanceof LP_FakePlayer) && LP_players[String(entity.uuid)] != null
}

function LP_remove(player) {
  if (!LP_isProtected(player)) return false
  player.setNoGravity(false)
  delete LP_players[String(player.uuid)]
  LP_active--
  return true
}

function LP_end(player, reason) {
  if (player instanceof LP_FakePlayer) console.info(`login_protection: ignored ${reason} from fake player ${player.username}`)
  if (!LP_isProtected(player)) return
  LP_release(player, LP_players[String(player.uuid)])
  LP_remove(player)
  console.info(`login_protection: ${player.username} ended (${reason})`)
  player.displayClientMessage(Text.gray(`Login protection ended (${reason}).`), true)
}

function LP_forgetAsTarget(player) {
  player.level.getEntitiesOfClass(LP_Mob, player.boundingBox.inflate(48)).forEach(mob => {
    if (mob.getTarget() == player) mob.setTarget(null)
    mob.getBrain().eraseMemory(LP_MemoryModuleType.ATTACK_TARGET)
  })
}

function LP_start(p) {
  const food = p.foodData
  LP_players[String(p.uuid)] = {
    player: p, hanging: true, ticks: 0, x: p.x, y: p.y, z: p.z, motion: LP_leftInAir[String(p.uuid)],
    air: p.getAirSupply(), fire: p.getRemainingFireTicks(), frozen: p.getTicksFrozen(),
    food: food.getFoodLevel(), saturation: food.getSaturationLevel(), exhaustion: food.getExhaustionLevel()
  }
  LP_active++
  console.info(`login_protection: ${p.username} started, active=${LP_active}`)
  p.setNoGravity(true)
  p.setKnownMovement(LP_UNSEEN)
  LP_forgetAsTarget(p)
}

function LP_restoreMotion(p, s) {
  p.setDeltaMovement(s.motion)
  p.hurtMarked = true
  console.info(`login_protection: ${p.username} speed restored ${s.motion}`)
  s.motion = null
  delete LP_leftInAir[String(p.uuid)]
}

function LP_showCounter(p, s) {
  p.connection.send(new LP_TitleTimes(0, 20, 5))
  p.connection.send(new LP_TitleText(Text.aqua(`${s.grace / 20} s`).bold()))
}

function LP_release(p, s) {
  if (!s.hanging) return
  s.hanging = false
  p.setNoGravity(false)
  p.connection.send(new LP_TitleClear(true))
  if (s.motion != null) LP_restoreMotion(p, s)
}

function LP_clientRuns(p, s) {
  const arrived = p.getKnownMovement().x() != LP_UNSEEN.x()
  p.setKnownMovement(LP_UNSEEN)
  if (!arrived || s.ticks <= LP_LOGIN_PACKET_TICKS) return false
  const runs = s.lastPacket != null && s.ticks - s.lastPacket <= LP_MAX_PACKET_GAP_TICKS
  s.lastPacket = s.ticks
  return runs
}

function LP_tick(p, s) {
  s.ticks++
  const dx = p.x - s.x
  const dy = p.y - s.y
  const dz = p.z - s.z
  const moved = dx * dx + dy * dy + dz * dz > LP_FIRST_MOVE_SQ
  if (s.grace == null && (LP_clientRuns(p, s) || moved)) {
    s.grace = LP_GRACE_TICKS
    console.info(`login_protection: ${p.username} client loaded ${s.ticks / 20} s after login, moved=${moved}`)
  }
  if (moved) LP_release(p, s)
  if (s.grace != null && --s.grace <= 0) return LP_end(p, '6 s after loading')
  if (s.grace == null && s.ticks > LP_MAX_TICKS) return LP_end(p, '80 s')
  p.setAirSupply(s.air)
  p.setRemainingFireTicks(s.fire)
  p.setTicksFrozen(s.frozen)
  p.resetFallDistance()
  const food = p.foodData
  food.setFoodLevel(s.food)
  food.setSaturation(s.saturation)
  food.setExhaustion(s.exhaustion)
  if (s.ticks % 20 == 0) LP_forgetAsTarget(p)
  if (s.hanging && s.grace != null && s.grace % 10 == 0) LP_showCounter(p, s)
}

function LP_onDamage(event) {
  const source = event.getSource()
  if (source.actual != null) LP_end(source.actual, 'you attacked')
  if (LP_isProtected(event.getEntity()) && !source.is(LP_DamageTypeTags.BYPASSES_INVULNERABILITY)) {
    event.setCanceled(true)
    console.info(`login_protection: blocked damage on ${event.getEntity().username}`)
  }
}

function LP_onLogout(p) {
  const id = String(p.uuid)
  if (LP_remove(p)) return
  if (p.onGround() || p.isPassenger()) {
    delete LP_leftInAir[id]
  } else {
    LP_leftInAir[id] = p.getKnownMovement()
  }
}

function LP_onPlayerTick(player) {
  const s = LP_players[String(player.uuid)]
  if (s != null) LP_tick(player, s)
}

PlayerEvents.loggedIn(event => {
  try {
    if (LP_failed) event.player.setNoGravity(false)
    else LP_start(event.player)
  } catch (e) { LP_fail(e) }
})

PlayerEvents.loggedOut(event => {
  if (LP_failed) return
  try { LP_onLogout(event.player) } catch (e) { LP_fail(e) }
})

ServerEvents.tick(event => {
  if (LP_active == 0) return
  try { event.server.getPlayerList().getPlayers().forEach(p => LP_onPlayerTick(p)) } catch (e) { LP_fail(e) }
})

BlockEvents.broken(event => {
  if (LP_active == 0) return
  try { LP_end(event.player, 'you broke a block') } catch (e) { LP_fail(e) }
})

BlockEvents.placed(event => {
  if (LP_active == 0) return
  try { if (event.entity != null && event.entity.isPlayer()) LP_end(event.entity, 'you placed a block') } catch (e) { LP_fail(e) }
})

ItemEvents.rightClicked(event => {
  if (LP_active == 0) return
  try { LP_end(event.player, 'you used an item') } catch (e) { LP_fail(e) }
})

BlockEvents.rightClicked(event => {
  if (LP_active == 0) return
  try { LP_end(event.player, 'you right clicked') } catch (e) { LP_fail(e) }
})

NativeEvents.onEvent(LP_DamageEvent, event => {
  if (LP_active == 0) return
  try { LP_onDamage(event) } catch (e) { LP_fail(e) }
})

NativeEvents.onEvent(LP_TargetEvent, event => {
  if (LP_active == 0) return
  try { if (LP_isProtected(event.getNewAboutToBeSetTarget())) event.setCanceled(true) } catch (e) { LP_fail(e) }
})

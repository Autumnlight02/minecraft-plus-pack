Minecraft Plus: Little Tiles Measure Tape Fix (minecraftplus_ltmeasurefix), SERVER ONLY, 2026-10-17

Bug: LittleTiles 1.6.0-pre232 (also not mentioned as fixed in pre233/pre234). Using the Little Measure Tape (box
modes) disconnected the player: "Failed to decode packet 'serverbound/minecraft:custom_payload'", caused by
"Failed decoding custom payload littletiles:7c: Attempted to load class creativecore/client/render/box/RenderBox for
invalid dist DEDICATED_SERVER". Decoding the measurement loads LittleMeasurementBox / LittleMeasurementSimpleBox; their
build() passes a LittleRenderBox where a RenderBox is expected, so the bytecode verifier loads the client-only RenderBox.

Fix: two mixins in the "server" section (dedicated server only) @Overwrite build() with an empty body (the server never
renders). An @Inject would keep the original bytecode and still fail verification. The mod logs a self-test at startup:
"[MinecraftPlus LT measure fix] LittleMeasurementBox loads on the server: OK".
Verified 2026-10-17 on a minimal server (CreativeCore + LittleTiles + this mod): without the mixins both classes fail with
the exact error from the live log; with them both load.

Build: ./build.sh -> ../../../server-mods/minecraftplus-littletiles-measure-fix-1.0.0.jar (stubs/ = compile-only empty
client classes, not packaged). Install: copy to the server's mods/ only. Remove when LittleTiles fixes it upstream.

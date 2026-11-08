Minecraft Plus: Create CT on Framed Blocks (mcplus_create_ct_camo) - CLIENT ONLY

Problem: a FramedBlocks block with a Create connected-texture camo (for example create:framed_glass) does not connect
its texture to neighbours.
1. Create's ConnectedTextureBehaviour.testConnection passes the block's own state to connectsTo, which compares
   state.getBlock() == other.getBlock(). Create resolves getAppearance only for the neighbour, so for a framed block
   "self" is the framed block, not its camo.
2. On cut faces (slopes etc.) FramedBlocks keeps camo CT data only for ModelProperty objects registered through the
   IMC "add_ct_property". Create's CTModel.CT_PROPERTY is private and not registered (FramedBlocks' own registration
   is switched off: CreateCompat.REGISTER_CT_PROPERTY = false). FramedBlocks uses that data as a quad cache key, so
   CTModel.CTData needs value equality.

Fix:
- ConnectedTextureBehaviourMixin: in testConnection, "self" becomes
  state.getAppearance(reader, currentPos, textureSide, reader.getBlockState(targetPos), targetPos). Plain blocks
  return themselves (no change). An AIR answer (FramedBlocks: this edge must not connect) keeps the original state.
- CTModelAccessor + CreateCtCamo: send CT_PROPERTY to FramedBlocks via IMC "add_ct_property".
  FramedBlocks' client config can turn it off per sender mod id (mcplus_create_ct_camo).
- CTDataMixin: equals/hashCode over CTData's indices.

Safety: client-only (@Mod dist CLIENT, mixins only in the "client" list), mixin config "required": false and
"defaultRequire": 0, so a changed Create/FramedBlocks only disables the fix. No refmap: NeoForge 1.21.1 runs with
Mojmap names and all targets are mod classes. Do not put the jar on a server.

Build: ./build.sh (javac from $HOME/mcplus-server/jdk; classpath = server libraries + Create and FramedBlocks jars).
Remove when Create or FramedBlocks handle camo CT themselves.

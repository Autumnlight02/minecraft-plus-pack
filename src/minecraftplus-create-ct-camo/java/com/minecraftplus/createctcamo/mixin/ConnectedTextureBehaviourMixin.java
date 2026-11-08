package com.minecraftplus.createctcamo.mixin;

import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.extensions.IBlockStateExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = ConnectedTextureBehaviour.class, remap = false)
public abstract class ConnectedTextureBehaviourMixin {
    @ModifyArg(
        method = "testConnection",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/foundation/block/connected/ConnectedTextureBehaviour;connectsTo(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/core/Direction;Lnet/minecraft/core/Direction;)Z"
        ),
        index = 0,
        require = 0
    )
    private BlockState mcplus$selfAppearance(
        BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos, BlockPos otherPos,
        Direction face, Direction primaryOffset, Direction secondaryOffset
    ) {
        // BlockState only implements IBlockStateExtension after NeoForge's runtime patches.
        BlockState appearance = ((IBlockStateExtension) (Object) state)
            .getAppearance(reader, pos, face, reader.getBlockState(otherPos), otherPos);
        // FramedBlocks answers AIR for edges that must not connect; AIR as "self" would match air neighbours.
        return appearance.isAir() ? state : appearance;
    }
}

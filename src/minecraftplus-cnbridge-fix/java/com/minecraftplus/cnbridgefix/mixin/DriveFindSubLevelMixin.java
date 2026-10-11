package com.minecraftplus.cnbridgefix.mixin;

import com.example.cnbridge.block.entity.DimensionalDriveBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// cnbridge 1.0.0 returned the first ship in the drive's dimension. Use the ship whose plot contains the drive.
@Mixin(value = DimensionalDriveBlockEntity.class, remap = false)
public abstract class DriveFindSubLevelMixin {
    @Inject(method = "findSubLevel", at = @At("HEAD"), cancellable = true)
    private static void minecraftplus$containingShip(Level level, BlockPos pos, CallbackInfoReturnable<ServerSubLevel> cir) {
        if (level == null) return;
        SubLevel ship = Sable.HELPER.getContaining(level, (Vec3i) pos);
        cir.setReturnValue(ship instanceof ServerSubLevel s ? s : null);
    }
}

package com.minecraftplus.grapplefix.mixin;

import com.yyon.grapplemod.entities.grapplehook.GrapplehookEntity;
import com.yyon.grapplemod.network.GrappleAttachPosMessage;
import com.yyon.grapplemod.network.GrappleNetwork;
import com.yyon.grapplemod.utils.Vec;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A block on a Sable ship really lives in the ship's plot, far away; Sable reports hits on it in plot coordinates.
 * The hook used that raw hit position as its anchor. Here the anchor is converted to the visible (world) position,
 * and while attached it is moved with the ship every tick.
 */
@Mixin(value = GrapplehookEntity.class, remap = false)
public abstract class GrapplehookOnShipMixin {
    @Unique
    private Vec3 minecraftplus$plotPos;

    @Inject(method = "serverAttach", at = @At("HEAD"))
    private void minecraftplus$attachToVisibleShip(BlockPos blockpos, Vec pos, Direction sideHit, CallbackInfo ci) {
        GrapplehookEntity self = (GrapplehookEntity) (Object) this;
        if (self.attached || blockpos == null || pos == null) return;
        SubLevelAccess ship = SableCompanion.INSTANCE.getContaining(self.level(), (Vec3i) blockpos);
        if (ship == null) return;
        Vec3 plot = new Vec3(pos.x, pos.y, pos.z);
        Vec3 world = ship.logicalPose().transformPosition(plot);
        minecraftplus$plotPos = plot;
        pos.x = world.x;
        pos.y = world.y;
        pos.z = world.z;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void minecraftplus$followShip(CallbackInfo ci) {
        GrapplehookEntity self = (GrapplehookEntity) (Object) this;
        if (minecraftplus$plotPos == null || !self.attached || self.level().isClientSide) return;
        SubLevelAccess ship = SableCompanion.INSTANCE.getContaining(self.level(), (Position) minecraftplus$plotPos);
        if (ship == null) return;
        Vec3 world = ship.logicalPose().transformPosition(minecraftplus$plotPos);
        if (self.thisPos != null && world.distanceToSqr(self.thisPos.toVec3d()) < 1.0E-4) return;
        self.thisPos = new Vec(world);
        self.setPosRaw(world.x, world.y, world.z);
        GrappleNetwork.sendToTracking(self.level(), new Vec(world),
                new GrappleAttachPosMessage(self.getId(), world.x, world.y, world.z));
    }
}

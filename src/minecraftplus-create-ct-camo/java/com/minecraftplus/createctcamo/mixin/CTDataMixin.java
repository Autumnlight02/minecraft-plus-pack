package com.minecraftplus.createctcamo.mixin;

import java.util.Arrays;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

// FramedBlocks uses the CT data as part of a quad cache key; identity equality would grow that cache forever.
@Mixin(targets = "com.simibubi.create.foundation.block.connected.CTModel$CTData", remap = false)
public abstract class CTDataMixin {
    @Shadow @Final private int[] indices;

    @Override
    public boolean equals(Object other) {
        return other instanceof CTDataMixin data && Arrays.equals(indices, data.indices);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(indices);
    }
}

package com.minecraftplus.createctcamo.mixin;

import com.simibubi.create.foundation.block.connected.CTModel;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = CTModel.class, remap = false)
public interface CTModelAccessor {
    @Accessor("CT_PROPERTY")
    static ModelProperty<?> mcplus$getCtProperty() {
        throw new AssertionError();
    }
}

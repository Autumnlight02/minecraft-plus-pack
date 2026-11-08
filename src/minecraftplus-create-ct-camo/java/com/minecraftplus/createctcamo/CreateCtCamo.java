package com.minecraftplus.createctcamo;

import com.minecraftplus.createctcamo.mixin.CTModelAccessor;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.neoforged.neoforge.client.model.data.ModelProperty;

@Mod(value = "mcplus_create_ct_camo", dist = Dist.CLIENT)
public final class CreateCtCamo {
    public CreateCtCamo(IEventBus modBus) {
        modBus.addListener(InterModEnqueueEvent.class, event -> registerCtProperty());
    }

    private static void registerCtProperty() {
        ModelProperty<?> property;
        try {
            property = CTModelAccessor.mcplus$getCtProperty();
        } catch (Throwable t) {
            LogUtils.getLogger().warn("Create CT property not reachable, framed cut faces keep plain camo textures", t);
            return;
        }
        InterModComms.sendTo("framedblocks", "add_ct_property", () -> property);
    }
}

package com.minecraftplus.ltmeasurefix;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

/** Self-test at startup: the two measurement classes must load on a dedicated server (they could not before the fix). */
@Mod("minecraftplus_ltmeasurefix")
public class MeasureFix {
    public MeasureFix() {
        if (FMLEnvironment.dist != Dist.DEDICATED_SERVER) return;
        for (String c : new String[] {"LittleMeasurementBox", "LittleMeasurementSimpleBox"}) {
            try {
                Class.forName("team.creative.littletiles.common.math.measure." + c);
                System.out.println("[MinecraftPlus LT measure fix] " + c + " loads on the server: OK");
            } catch (Throwable t) {
                System.err.println("[MinecraftPlus LT measure fix] " + c + " still fails to load: " + t);
            }
        }
    }
}

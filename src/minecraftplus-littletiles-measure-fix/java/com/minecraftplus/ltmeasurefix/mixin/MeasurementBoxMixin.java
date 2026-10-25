package com.minecraftplus.ltmeasurefix.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;

// Dedicated server only (mixins.json "server"). LittleMeasurementBox.build passes a LittleRenderBox where a creativecore
// RenderBox is expected, so the bytecode verifier loads RenderBox, which is client-only: loading the class fails and the
// server drops the player whose Measure Tape packet (littletiles:7c) created such a measurement. The server never
// renders, so the body is replaced with nothing. An @Inject would keep the original bytecode and still fail verification.
@Mixin(targets = "team.creative.littletiles.common.math.measure.LittleMeasurementBox", remap = false)
public abstract class MeasurementBoxMixin {
    /**
     * @author Minecraft Plus
     * @reason client-only rendering; its bytecode cannot be verified on a dedicated server
     */
    @Overwrite
    public void build(PreviewRenderer renderer, PoseStack pose, BufferBuilder builder) {
    }
}

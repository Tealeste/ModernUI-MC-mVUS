package icyllis.modernui.mc.text.mixin;

import icyllis.modernui.mc.MultiBufferSourceCompat;
import icyllis.modernui.mc.text.TextLayoutEngine;
import icyllis.modernui.mc.text.TextRenderType;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {

    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    @Inject(method = "renderLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V"))
    private void endTextBatch(CallbackInfo ci) {
        if (TextLayoutEngine.sUseTextShadersInWorld) {
            Object firstSDFFillType = TextRenderType.getFirstSDFFillType();
            Object firstSDFStrokeType = TextRenderType.getFirstSDFStrokeType();
            MultiBufferSourceCompat.endBatch(renderBuffers.bufferSource(), firstSDFFillType);
            MultiBufferSourceCompat.endBatch(renderBuffers.bufferSource(), firstSDFStrokeType);
        }
    }
}

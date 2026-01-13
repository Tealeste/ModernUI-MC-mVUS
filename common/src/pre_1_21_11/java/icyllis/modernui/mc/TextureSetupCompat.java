package icyllis.modernui.mc;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.gui.render.TextureSetup;

public final class TextureSetupCompat {

    private TextureSetupCompat() {
    }

    public static TextureSetup singleTexture(GpuTextureView view, Object sampler) {
        return TextureSetup.singleTexture(view);
    }

    public static TextureSetup singleTextureWithLightmap(GpuTextureView view, Object sampler) {
        return TextureSetup.singleTextureWithLightmap(view);
    }
}

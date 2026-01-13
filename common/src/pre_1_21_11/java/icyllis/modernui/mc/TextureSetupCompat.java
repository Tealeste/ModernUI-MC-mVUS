package icyllis.modernui.mc;

import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.gui.render.TextureSetup;

public final class TextureSetupCompat {

    private TextureSetupCompat() {
    }

    public static TextureSetup singleTexture(GpuTextureView view, Object sampler) {
        applyTextureFilter(view, sampler);
        return TextureSetup.singleTexture(view);
    }

    public static TextureSetup singleTextureWithLightmap(GpuTextureView view, Object sampler) {
        applyTextureFilter(view, sampler);
        return TextureSetup.singleTextureWithLightmap(view);
    }

    private static void applyTextureFilter(GpuTextureView view, Object sampler) {
        if (!(sampler instanceof SamplerCompat.TextureFilter filter)) {
            return;
        }
        GpuTexture texture = view.texture();
        texture.setAddressMode(AddressMode.CLAMP_TO_EDGE);
        texture.setTextureFilter(filter.filterMode(), filter.useMipmaps());
    }
}

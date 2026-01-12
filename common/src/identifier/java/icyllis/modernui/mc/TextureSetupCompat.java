package icyllis.modernui.mc;

import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;

import java.lang.reflect.Constructor;

public final class TextureSetupCompat {

    private static final Constructor<TextureSetup> CTOR_VIEWS =
            resolveCtor(GpuTextureView.class, GpuTextureView.class, GpuTextureView.class);
    private static final Constructor<TextureSetup> CTOR_VIEWS_AND_SAMPLERS =
            resolveCtor(GpuTextureView.class, GpuTextureView.class, GpuTextureView.class,
                    GpuSampler.class, GpuSampler.class, GpuSampler.class);

    private TextureSetupCompat() {
    }

    public static TextureSetup singleTexture(GpuTextureView view, Object sampler) {
        try {
            if (CTOR_VIEWS_AND_SAMPLERS != null) {
                return CTOR_VIEWS_AND_SAMPLERS.newInstance(view, null, null, (GpuSampler) sampler, null, null);
            }
            if (CTOR_VIEWS != null) {
                return CTOR_VIEWS.newInstance(view, null, null);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct TextureSetup (singleTexture)", e);
        }
        throw new IllegalStateException("No compatible TextureSetup constructor found");
    }

    public static TextureSetup singleTextureWithLightmap(GpuTextureView view, Object sampler) {
        try {
            GpuTextureView lightmapView = Minecraft.getInstance().gameRenderer.lightTexture().getTextureView();
            if (CTOR_VIEWS_AND_SAMPLERS != null) {
                Object lightmapSampler = SamplerCompat.clampToEdge(FilterMode.LINEAR);
                return CTOR_VIEWS_AND_SAMPLERS.newInstance(view, null, lightmapView, (GpuSampler) sampler, null, lightmapSampler);
            }
            if (CTOR_VIEWS != null) {
                return CTOR_VIEWS.newInstance(view, null, lightmapView);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct TextureSetup (singleTextureWithLightmap)", e);
        }
        throw new IllegalStateException("No compatible TextureSetup constructor found");
    }

    @SuppressWarnings("unchecked")
    private static Constructor<TextureSetup> resolveCtor(Class<?>... parameterTypes) {
        try {
            return (Constructor<TextureSetup>) TextureSetup.class.getConstructor(parameterTypes);
        } catch (Exception ignored) {
            return null;
        }
    }
}

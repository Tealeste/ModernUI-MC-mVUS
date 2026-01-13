package icyllis.modernui.mc;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class SamplerCompat {

    /**
     * 1.21.10 and older do not expose a {@code SamplerCache} API and instead store filtering state on {@code GpuTexture}.
     * This object acts as a minimal cross-version carrier for "sampler intent" (filter + mipmaps) so patch-specific
     * code can apply it where appropriate (e.g., in {@code TextureSetupCompat}).
     */
    public record TextureFilter(FilterMode filterMode, boolean useMipmaps) {
    }

    private static final Method GET_SAMPLER_CACHE = resolveMethod(RenderSystem.class, "getSamplerCache");
    private static final Method CLAMP_TO_EDGE_1;
    private static final Method CLAMP_TO_EDGE_2;
    private static final Method CREATE_SAMPLER;

    private static final boolean HAS_TEXTURE_FILTER_API = hasTextureFilterApi();
    private static final Object FALLBACK_NEAREST = new TextureFilter(FilterMode.NEAREST, /*useMipmaps*/ false);
    private static final Object FALLBACK_LINEAR = new TextureFilter(FilterMode.LINEAR, /*useMipmaps*/ false);
    private static final Object FALLBACK_LINEAR_MIPMAPS = new TextureFilter(FilterMode.LINEAR, /*useMipmaps*/ true);

    static {
        Method clamp1 = null;
        Method clamp2 = null;
        Method createSampler = null;
        if (GET_SAMPLER_CACHE != null) {
            Class<?> cacheClass = GET_SAMPLER_CACHE.getReturnType();
            clamp1 = resolveMethod(cacheClass, "getClampToEdge", FilterMode.class);
            clamp2 = resolveMethod(cacheClass, "getClampToEdge", FilterMode.class, boolean.class);
            createSampler = resolveCreateSampler(cacheClass);
        }
        CLAMP_TO_EDGE_1 = clamp1;
        CLAMP_TO_EDGE_2 = clamp2;
        CREATE_SAMPLER = createSampler;
    }

    private SamplerCompat() {
    }

    public static boolean isSupported() {
        boolean samplerCacheSupported = GET_SAMPLER_CACHE != null && (CLAMP_TO_EDGE_1 != null || CREATE_SAMPLER != null);
        return samplerCacheSupported || HAS_TEXTURE_FILTER_API;
    }

    public static Object clampToEdge(FilterMode filterMode) {
        try {
            if (GET_SAMPLER_CACHE == null) {
                return HAS_TEXTURE_FILTER_API ? fallbackFilter(filterMode, /*useMipmaps*/ false) : null;
            }
            Object cache = GET_SAMPLER_CACHE.invoke(null);
            if (CLAMP_TO_EDGE_1 != null) {
                return CLAMP_TO_EDGE_1.invoke(cache, filterMode);
            }
            if (CREATE_SAMPLER != null) {
                return CREATE_SAMPLER.invoke(
                        cache,
                        AddressMode.CLAMP_TO_EDGE,
                        AddressMode.CLAMP_TO_EDGE,
                        filterMode,
                        filterMode,
                        /*useMipmaps*/ false
                );
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get clamp-to-edge sampler", e);
        }
    }

    public static Object clampToEdge(FilterMode filterMode, boolean useMipmaps) {
        try {
            if (GET_SAMPLER_CACHE == null) {
                return HAS_TEXTURE_FILTER_API ? fallbackFilter(filterMode, useMipmaps) : null;
            }
            Object cache = GET_SAMPLER_CACHE.invoke(null);
            if (CLAMP_TO_EDGE_2 != null) {
                return CLAMP_TO_EDGE_2.invoke(cache, filterMode, useMipmaps);
            }
            if (CLAMP_TO_EDGE_1 != null) {
                return CLAMP_TO_EDGE_1.invoke(cache, filterMode);
            }
            if (CREATE_SAMPLER != null) {
                return CREATE_SAMPLER.invoke(
                        cache,
                        AddressMode.CLAMP_TO_EDGE,
                        AddressMode.CLAMP_TO_EDGE,
                        filterMode,
                        filterMode,
                        useMipmaps
                );
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get clamp-to-edge sampler", e);
        }
    }

    private static Object fallbackFilter(FilterMode filterMode, boolean useMipmaps) {
        if (useMipmaps && filterMode == FilterMode.LINEAR) {
            return FALLBACK_LINEAR_MIPMAPS;
        }
        if (filterMode == FilterMode.LINEAR) {
            return FALLBACK_LINEAR;
        }
        return FALLBACK_NEAREST;
    }

    private static boolean hasTextureFilterApi() {
        try {
            Class<?> gpuTexture = Class.forName("com.mojang.blaze3d.textures.GpuTexture");
            gpuTexture.getMethod("setTextureFilter", FilterMode.class, boolean.class);
            gpuTexture.getMethod("setAddressMode", AddressMode.class);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Method resolveCreateSampler(Class<?> cacheClass) {
        for (Method method : cacheClass.getMethods()) {
            Method candidate = tryMatchCreateSampler(cacheClass, method);
            if (candidate != null) {
                return candidate;
            }
        }
        for (Method method : cacheClass.getDeclaredMethods()) {
            Method candidate = tryMatchCreateSampler(cacheClass, method);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private static Method tryMatchCreateSampler(Class<?> cacheClass, Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            return null;
        }
        if (method.getReturnType() == void.class || method.getParameterCount() != 5) {
            return null;
        }
        Class<?>[] params = method.getParameterTypes();
        if (params[0] != AddressMode.class || params[1] != AddressMode.class) {
            return null;
        }
        if (params[2] != FilterMode.class || params[3] != FilterMode.class) {
            return null;
        }
        if (params[4] != boolean.class) {
            return null;
        }
        if (method.getReturnType() == cacheClass) {
            return null;
        }
        method.setAccessible(true);
        return method;
    }

    private static Method resolveMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
        try {
            return owner.getMethod(name, parameterTypes);
        } catch (Exception ignored) {
            return null;
        }
    }
}

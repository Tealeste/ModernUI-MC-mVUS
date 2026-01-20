package icyllis.modernui.mc;

import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;

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
            GpuTextureView lightmapView = resolveLightmapView();
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

    private static GpuTextureView resolveLightmapView() throws ReflectiveOperationException {
        Object gameRenderer = Minecraft.getInstance().gameRenderer;
        LightmapViewResolver resolver = LIGHTMAP_VIEW_RESOLVER;
        if (resolver == null) {
            synchronized (TextureSetupCompat.class) {
                resolver = LIGHTMAP_VIEW_RESOLVER;
                if (resolver == null) {
                    resolver = resolveLightmapViewResolver(gameRenderer);
                    LIGHTMAP_VIEW_RESOLVER = resolver;
                }
            }
        }
        return resolver.resolve(gameRenderer);
    }

    @FunctionalInterface
    private interface LightmapViewResolver {
        GpuTextureView resolve(Object gameRenderer) throws ReflectiveOperationException;
    }

    private static volatile LightmapViewResolver LIGHTMAP_VIEW_RESOLVER;

    private static LightmapViewResolver resolveLightmapViewResolver(Object gameRenderer) throws ReflectiveOperationException {
        Class<?> rendererClass = gameRenderer.getClass();

        // 1.21.x uses a LightTexture manager; prefer it when present to avoid accidentally
        // picking other 16x16 textures (e.g., overlay) via heuristic reflection.
        LightmapViewResolver viaLightTexture = findLightmapViewViaLightTexture(gameRenderer, rendererClass);
        if (viaLightTexture != null) {
            return viaLightTexture;
        }

        // 26.1+ may expose a direct lightmap view getter; prefer it when present.
        LightmapViewResolver directViewGetter = findLightmapViewViaDirectGetter(gameRenderer, rendererClass.getMethods());
        if (directViewGetter != null) {
            return directViewGetter;
        }
        LightmapViewResolver directDeclaredViewGetter = findLightmapViewViaDirectGetter(gameRenderer, rendererClass.getDeclaredMethods());
        if (directDeclaredViewGetter != null) {
            return directDeclaredViewGetter;
        }

        // 1.21.11 uses a light texture manager; find a holder (field/method) that can yield a GpuTextureView.
        LightmapViewResolver viaPublicHolder = findLightmapViewViaHolder(gameRenderer, rendererClass.getMethods());
        if (viaPublicHolder != null) {
            return viaPublicHolder;
        }
        LightmapViewResolver viaDeclaredHolder = findLightmapViewViaHolder(gameRenderer, rendererClass.getDeclaredMethods());
        if (viaDeclaredHolder != null) {
            return viaDeclaredHolder;
        }
        LightmapViewResolver viaPublicFields = findLightmapViewViaHolder(gameRenderer, rendererClass.getFields());
        if (viaPublicFields != null) {
            return viaPublicFields;
        }
        LightmapViewResolver viaDeclaredFields = findLightmapViewViaHolder(gameRenderer, rendererClass.getDeclaredFields());
        if (viaDeclaredFields != null) {
            return viaDeclaredFields;
        }

        throw new NoSuchMethodException("Unable to resolve lightmap texture view from " + rendererClass.getName());
    }

    private static LightmapViewResolver findLightmapViewViaLightTexture(Object gameRenderer, Class<?> rendererClass) {
        Method holderGetter = findNoArgMethod(rendererClass, "lightTexture");
        if (holderGetter == null) {
            return null;
        }

        Class<?> holderType = holderGetter.getReturnType();
        if (holderType == void.class || holderType.isPrimitive()) {
            return null;
        }

        Method viewGetter = findNoArgMethod(holderType, "getTextureView");
        if (viewGetter == null) {
            viewGetter = findNoArgGetter(holderType, GpuTextureView.class);
        }
        if (viewGetter == null) {
            return null;
        }

        holderGetter.setAccessible(true);
        viewGetter.setAccessible(true);

        final Method holderGetterFinal = holderGetter;
        final Method viewGetterFinal = viewGetter;

        Object holder;
        try {
            holder = holderGetter.invoke(gameRenderer);
        } catch (ReflectiveOperationException e) {
            return null;
        }
        if (holder == null) {
            return null;
        }

        GpuTextureView view;
        try {
            view = (GpuTextureView) viewGetter.invoke(holder);
        } catch (ReflectiveOperationException e) {
            return null;
        }
        if (!isLightmapSized(view)) {
            return null;
        }

        return renderer -> {
            Object currentHolder = holderGetterFinal.invoke(renderer);
            if (currentHolder == null) {
                throw new NullPointerException("LightTexture holder is null");
            }
            return (GpuTextureView) viewGetterFinal.invoke(currentHolder);
        };
    }

    private static Method findNoArgMethod(Class<?> owner, String methodName) {
        try {
            Method method = owner.getMethod(methodName);
            if (method.getParameterCount() == 0 && !Modifier.isStatic(method.getModifiers()) && method.getReturnType() != void.class) {
                return method;
            }
        } catch (NoSuchMethodException ignored) {
        }
        try {
            Method method = owner.getDeclaredMethod(methodName);
            if (method.getParameterCount() == 0 && !Modifier.isStatic(method.getModifiers()) && method.getReturnType() != void.class) {
                return method;
            }
        } catch (NoSuchMethodException ignored) {
        }
        return null;
    }

    private static Method findNoArgGetter(Class<?> owner, Class<?> returnType) {
        for (Method method : owner.getMethods()) {
            if (isNoArgGetter(method, returnType)) {
                return method;
            }
        }
        for (Method method : owner.getDeclaredMethods()) {
            if (isNoArgGetter(method, returnType)) {
                return method;
            }
        }
        return null;
    }

    private static boolean isNoArgGetter(Method method, Class<?> returnType) {
        if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0) {
            return false;
        }
        if (method.getReturnType() == void.class) {
            return false;
        }
        return returnType.isAssignableFrom(method.getReturnType());
    }

    private static LightmapViewResolver findLightmapViewViaDirectGetter(Object gameRenderer, Method[] candidates) {
        LightmapViewResolver fallback = null;
        for (Method viewGetter : candidates) {
            if (!isNoArgGetter(viewGetter, GpuTextureView.class)) {
                continue;
            }

            viewGetter.setAccessible(true);
            GpuTextureView view;
            try {
                view = (GpuTextureView) viewGetter.invoke(gameRenderer);
            } catch (ReflectiveOperationException e) {
                continue;
            }

            LightmapViewResolver candidate = renderer -> (GpuTextureView) viewGetter.invoke(renderer);
            if (isLightmapSized(view)) {
                return candidate;
            }
            if (fallback == null && view != null) {
                fallback = candidate;
            }
        }
        return fallback;
    }

    private static LightmapViewResolver findLightmapViewViaHolder(Object gameRenderer, Method[] candidates) {
        LightmapViewResolver fallback = null;
        for (Method holderGetter : candidates) {
            if (Modifier.isStatic(holderGetter.getModifiers()) || holderGetter.getParameterCount() != 0) {
                continue;
            }
            Class<?> holderType = holderGetter.getReturnType();
            if (holderType == void.class || holderType.isPrimitive() || GpuTextureView.class.isAssignableFrom(holderType)) {
                continue;
            }

            Method viewGetter = findNoArgGetter(holderType, GpuTextureView.class);
            if (viewGetter == null) {
                continue;
            }

            holderGetter.setAccessible(true);
            viewGetter.setAccessible(true);

            Object holder;
            try {
                holder = holderGetter.invoke(gameRenderer);
            } catch (ReflectiveOperationException e) {
                continue;
            }
            if (holder == null) {
                continue;
            }

            GpuTextureView view;
            try {
                view = (GpuTextureView) viewGetter.invoke(holder);
            } catch (ReflectiveOperationException e) {
                continue;
            }

            LightmapViewResolver candidate = renderer -> {
                Object currentHolder = holderGetter.invoke(renderer);
                if (currentHolder == null) {
                    throw new NullPointerException("Lightmap holder is null");
                }
                return (GpuTextureView) viewGetter.invoke(currentHolder);
            };
            if (isLightmapSized(view)) {
                return candidate;
            }
            if (fallback == null && view != null) {
                fallback = candidate;
            }
        }
        return fallback;
    }

    private static LightmapViewResolver findLightmapViewViaHolder(Object gameRenderer, Field[] candidates) {
        LightmapViewResolver fallback = null;
        for (Field holderField : candidates) {
            if (Modifier.isStatic(holderField.getModifiers())) {
                continue;
            }
            Class<?> holderType = holderField.getType();
            if (holderType.isPrimitive() || GpuTextureView.class.isAssignableFrom(holderType)) {
                continue;
            }

            Method viewGetter = findNoArgGetter(holderType, GpuTextureView.class);
            if (viewGetter == null) {
                continue;
            }

            holderField.setAccessible(true);
            viewGetter.setAccessible(true);

            Object holder;
            try {
                holder = holderField.get(gameRenderer);
            } catch (IllegalAccessException e) {
                continue;
            }
            if (holder == null) {
                continue;
            }

            GpuTextureView view;
            try {
                view = (GpuTextureView) viewGetter.invoke(holder);
            } catch (ReflectiveOperationException e) {
                continue;
            }

            LightmapViewResolver candidate = renderer -> {
                Object currentHolder = holderField.get(renderer);
                if (currentHolder == null) {
                    throw new NullPointerException("Lightmap holder is null");
                }
                return (GpuTextureView) viewGetter.invoke(currentHolder);
            };
            if (isLightmapSized(view)) {
                return candidate;
            }
            if (fallback == null && view != null) {
                fallback = candidate;
            }
        }
        return fallback;
    }

    private static boolean isLightmapSized(GpuTextureView view) {
        if (view == null) {
            return false;
        }
        try {
            GpuTexture texture = view.texture();
            if (texture == null) {
                return false;
            }

            // Prefer identifying the lightmap via the underlying texture label, since there can be
            // multiple 16x16 GPU textures in the renderer (e.g., the entity overlay texture).
            //
            // Vanilla labels (observed):
            // - 1.21.x: "Light Texture"
            // - 26.1+: "UI Lightmap"
            String label = texture.getLabel();
            if (label != null) {
                String lower = label.toLowerCase(Locale.ROOT);
                return lower.contains("light texture") || lower.contains("ui lightmap");
            }

            // Fallback heuristic when labels are unavailable.
            return texture.getWidth(0) == 16 && texture.getHeight(0) == 16;
        } catch (Throwable ignored) {
            return false;
        }
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

package icyllis.modernui.mc;

import com.mojang.blaze3d.pipeline.RenderPipeline;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Supplier;

public final class RenderTypeCompat {

    private static final Impl IMPL = resolveImpl();

    private RenderTypeCompat() {
    }

    @SuppressWarnings("unchecked")
    public static <RT> RT create(String name,
                                 int bufferSize,
                                 boolean affectsCrumbling,
                                 boolean sortOnUpload,
                                 RenderPipeline renderPipeline,
                                 @Nullable Object texture,
                                 @Nullable Supplier<?> sampler,
                                 boolean lightmap) {
        return (RT) IMPL.create(name, bufferSize, affectsCrumbling, sortOnUpload, renderPipeline, texture, sampler, lightmap);
    }

    private interface Impl {
        Object create(String name,
                      int bufferSize,
                      boolean affectsCrumbling,
                      boolean sortOnUpload,
                      RenderPipeline renderPipeline,
                      @Nullable Object texture,
                      @Nullable Supplier<?> sampler,
                      boolean lightmap);
    }

    private static Impl resolveImpl() {
        if (tryLoadClass("net.minecraft.client.renderer.rendertype.RenderSetup", "net.minecraft.class_12247") != null) {
            return new RenderSetupImpl();
        }
        return new CompositeStateImpl();
    }

    private static final class RenderSetupImpl implements Impl {

        private final Method renderSetupBuilder;
        private final Method builderUseLightmap;
        private final Method builderAffectsCrumbling;
        private final Method builderSortOnUpload;
        private final Method builderBufferSize;
        private final Method builderWithTexture;
        private final Method builderWithTextureAndSampler;
        private final Method builderCreateRenderSetup;
        private final Method renderTypeCreate;

        private RenderSetupImpl() {
            try {
                Class<?> renderSetupClass = requireClass(
                        "net.minecraft.client.renderer.rendertype.RenderSetup",
                        "net.minecraft.class_12247"
                );
                Class<?> renderTypeClass = requireClass(
                        "net.minecraft.client.renderer.rendertype.RenderType",
                        "net.minecraft.class_1921"
                );

                renderSetupBuilder = resolveMethod(renderSetupClass, "builder", "method_75927", RenderPipeline.class);
                Class<?> builderClass = renderSetupBuilder.getReturnType();

                builderUseLightmap = resolveMethod(builderClass, "useLightmap", "method_75928");
                builderAffectsCrumbling = resolveMethod(builderClass, "affectsCrumbling", "method_75936");
                builderSortOnUpload = resolveMethod(builderClass, "sortOnUpload", "method_75937");
                builderBufferSize = resolveMethod(builderClass, "bufferSize", "method_75929", int.class);

                Class<?> idClass = ResourceIdCompat.idClass();
                builderWithTexture = resolveWithTexture(builderClass, idClass, /*withSampler*/ false);
                builderWithTextureAndSampler = resolveWithTexture(builderClass, idClass, /*withSampler*/ true);

                builderCreateRenderSetup = resolveNoArgReturn(renderSetupClass, builderClass);
                renderTypeCreate = resolveStaticCreate(renderTypeClass, renderSetupClass);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to initialize RenderTypeCompat (RenderSetup)", e);
            }
        }

        @Override
        public Object create(String name,
                             int bufferSize,
                             boolean affectsCrumbling,
                             boolean sortOnUpload,
                             RenderPipeline renderPipeline,
                             @Nullable Object texture,
                             @Nullable Supplier<?> sampler,
                             boolean lightmap) {
            try {
                Object builder = renderSetupBuilder.invoke(null, renderPipeline);
                if (lightmap) {
                    builderUseLightmap.invoke(builder);
                }
                if (affectsCrumbling) {
                    builderAffectsCrumbling.invoke(builder);
                }
                if (sortOnUpload) {
                    builderSortOnUpload.invoke(builder);
                }
                builderBufferSize.invoke(builder, bufferSize);
                if (texture != null) {
                    if (sampler != null) {
                        builderWithTextureAndSampler.invoke(builder, "Sampler0", texture, sampler);
                    } else {
                        builderWithTexture.invoke(builder, "Sampler0", texture);
                    }
                }
                Object setup = builderCreateRenderSetup.invoke(builder);
                return renderTypeCreate.invoke(null, name, setup);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create RenderType via RenderSetup", e);
            }
        }

        private static Method resolveWithTexture(Class<?> builderClass, Class<?> idClass, boolean withSampler) {
            int expectedParams = withSampler ? 3 : 2;
            for (Method method : builderClass.getMethods()) {
                if (Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (method.getReturnType() != builderClass || method.getParameterCount() != expectedParams) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params[0] != String.class || params[1] != idClass) {
                    continue;
                }
                if (withSampler && !Supplier.class.isAssignableFrom(params[2])) {
                    continue;
                }
                method.setAccessible(true);
                return method;
            }
            for (Method method : builderClass.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (method.getReturnType() != builderClass || method.getParameterCount() != expectedParams) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params[0] != String.class || params[1] != idClass) {
                    continue;
                }
                if (withSampler && !Supplier.class.isAssignableFrom(params[2])) {
                    continue;
                }
                method.setAccessible(true);
                return method;
            }
            throw new IllegalStateException("No compatible RenderSetupBuilder#withTexture overload found");
        }

        private static Method resolveNoArgReturn(Class<?> returnType, Class<?> builderClass) {
            for (Method method : builderClass.getMethods()) {
                if (Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (method.getParameterCount() != 0 || method.getReturnType() != returnType) {
                    continue;
                }
                method.setAccessible(true);
                return method;
            }
            for (Method method : builderClass.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (method.getParameterCount() != 0 || method.getReturnType() != returnType) {
                    continue;
                }
                method.setAccessible(true);
                return method;
            }
            throw new IllegalStateException("No compatible RenderSetupBuilder factory method found");
        }

        private static Method resolveStaticCreate(Class<?> renderTypeClass, Class<?> renderSetupClass) {
            for (Method method : renderTypeClass.getMethods()) {
                if (!Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (method.getReturnType() != renderTypeClass || method.getParameterCount() != 2) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params[0] != String.class || params[1] != renderSetupClass) {
                    continue;
                }
                method.setAccessible(true);
                return method;
            }
            for (Method method : renderTypeClass.getDeclaredMethods()) {
                if (!Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (method.getReturnType() != renderTypeClass || method.getParameterCount() != 2) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params[0] != String.class || params[1] != renderSetupClass) {
                    continue;
                }
                method.setAccessible(true);
                return method;
            }
            throw new IllegalStateException("No compatible RenderType#create(String,RenderSetup) overload found");
        }
    }

    private static final class CompositeStateImpl implements Impl {

        private final Method compositeStateBuilderFactory;
        private final Method compositeStateBuilderSetTextureState;
        private final Method compositeStateBuilderSetLightmapState;
        private final Method compositeStateBuilderSetOverlayState;
        private final Method compositeStateBuilderSetLayeringState;
        private final Method compositeStateBuilderSetOutputState;
        private final Method compositeStateBuilderSetTexturingState;
        private final Method compositeStateBuilderSetLineState;
        private final Method compositeStateBuilderCreate;

        private final Object noTexture;
        private final Object lightmap;
        private final Object noLightmap;
        private final Object noOverlay;
        private final Object noLayering;
        private final Object mainTarget;
        private final Object defaultTexturing;
        private final Object defaultLine;

        private final Constructor<?> textureStateShardCtor;
        private final Method renderTypeCreate;

        private CompositeStateImpl() {
            try {
                Class<?> renderTypeClass = Class.forName("net.minecraft.client.renderer.RenderType");
                Class<?> compositeStateClass = Class.forName("net.minecraft.client.renderer.RenderType$CompositeState");
                Class<?> compositeStateBuilderClass = Class.forName("net.minecraft.client.renderer.RenderType$CompositeState$CompositeStateBuilder");

                compositeStateBuilderFactory = compositeStateClass.getMethod("builder");
                compositeStateBuilderSetTextureState = compositeStateBuilderClass.getMethod("setTextureState",
                        Class.forName("net.minecraft.client.renderer.RenderStateShard$EmptyTextureStateShard"));
                compositeStateBuilderSetLightmapState = compositeStateBuilderClass.getMethod("setLightmapState",
                        Class.forName("net.minecraft.client.renderer.RenderStateShard$LightmapStateShard"));
                compositeStateBuilderSetOverlayState = compositeStateBuilderClass.getMethod("setOverlayState",
                        Class.forName("net.minecraft.client.renderer.RenderStateShard$OverlayStateShard"));
                compositeStateBuilderSetLayeringState = compositeStateBuilderClass.getMethod("setLayeringState",
                        Class.forName("net.minecraft.client.renderer.RenderStateShard$LayeringStateShard"));
                compositeStateBuilderSetOutputState = compositeStateBuilderClass.getMethod("setOutputState",
                        Class.forName("net.minecraft.client.renderer.RenderStateShard$OutputStateShard"));
                compositeStateBuilderSetTexturingState = compositeStateBuilderClass.getMethod("setTexturingState",
                        Class.forName("net.minecraft.client.renderer.RenderStateShard$TexturingStateShard"));
                compositeStateBuilderSetLineState = compositeStateBuilderClass.getMethod("setLineState",
                        Class.forName("net.minecraft.client.renderer.RenderStateShard$LineStateShard"));
                compositeStateBuilderCreate = compositeStateBuilderClass.getMethod("createCompositeState", boolean.class);

                Class<?> renderStateShardClass = Class.forName("net.minecraft.client.renderer.RenderStateShard");
                noTexture = getStatic(renderStateShardClass, "NO_TEXTURE");
                lightmap = getStatic(renderStateShardClass, "LIGHTMAP");
                noLightmap = getStatic(renderStateShardClass, "NO_LIGHTMAP");
                noOverlay = getStatic(renderStateShardClass, "NO_OVERLAY");
                noLayering = getStatic(renderStateShardClass, "NO_LAYERING");
                mainTarget = getStatic(renderStateShardClass, "MAIN_TARGET");
                defaultTexturing = getStatic(renderStateShardClass, "DEFAULT_TEXTURING");
                defaultLine = getStatic(renderStateShardClass, "DEFAULT_LINE");

                Class<?> textureStateShardClass = Class.forName("net.minecraft.client.renderer.RenderStateShard$TextureStateShard");
                Class<?> resourceLocationClass = Class.forName("net.minecraft.resources.ResourceLocation");
                textureStateShardCtor = textureStateShardClass.getConstructor(resourceLocationClass, boolean.class);

                renderTypeCreate = resolveRenderTypeCreate(renderTypeClass, compositeStateClass);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to initialize RenderTypeCompat (CompositeState)", e);
            }
        }

        @Override
        public Object create(String name,
                             int bufferSize,
                             boolean affectsCrumbling,
                             boolean sortOnUpload,
                             RenderPipeline renderPipeline,
                             @Nullable Object texture,
                             @Nullable Supplier<?> sampler,
                             boolean lightmapEnabled) {
            try {
                Object builder = compositeStateBuilderFactory.invoke(null);
                compositeStateBuilderSetTextureState.invoke(builder, texture != null ? textureState(texture) : noTexture);
                compositeStateBuilderSetLightmapState.invoke(builder, lightmapEnabled ? lightmap : noLightmap);
                compositeStateBuilderSetOverlayState.invoke(builder, noOverlay);
                compositeStateBuilderSetLayeringState.invoke(builder, noLayering);
                compositeStateBuilderSetOutputState.invoke(builder, mainTarget);
                compositeStateBuilderSetTexturingState.invoke(builder, defaultTexturing);
                compositeStateBuilderSetLineState.invoke(builder, defaultLine);
                Object compositeState = compositeStateBuilderCreate.invoke(builder, /*outline*/ false);

                return renderTypeCreate.invoke(null, name, bufferSize, affectsCrumbling, sortOnUpload, renderPipeline, compositeState);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create RenderType via CompositeState", e);
            }
        }

        private Object textureState(Object texture) throws Exception {
            return textureStateShardCtor.newInstance(texture, /*mipmap*/ false);
        }

        private static Method resolveRenderTypeCreate(Class<?> renderTypeClass, Class<?> compositeStateClass) {
            for (Method method : renderTypeClass.getDeclaredMethods()) {
                if (!method.getName().equals("create") || method.getParameterCount() != 6) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params[0] != String.class || params[1] != int.class) {
                    continue;
                }
                if (params[2] != boolean.class || params[3] != boolean.class) {
                    continue;
                }
                if (!RenderPipeline.class.isAssignableFrom(params[4])) {
                    continue;
                }
                if (params[5] != compositeStateClass) {
                    continue;
                }
                method.setAccessible(true);
                return method;
            }
            throw new IllegalStateException("No compatible RenderType#create(String,int,boolean,boolean,RenderPipeline,CompositeState) overload found");
        }

        private static Object getStatic(Class<?> owner, String fieldName) throws Exception {
            Field field = owner.getField(fieldName);
            return field.get(null);
        }
    }

    private static Class<?> tryLoadClass(String namedClassName, String intermediaryClassName) {
        try {
            return Class.forName(namedClassName);
        } catch (Throwable ignored) {
        }
        try {
            return Class.forName(intermediaryClassName);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Class<?> requireClass(String namedClassName, String intermediaryClassName) {
        Class<?> clazz = tryLoadClass(namedClassName, intermediaryClassName);
        if (clazz == null) {
            throw new IllegalStateException("Missing required class: " + namedClassName + " / " + intermediaryClassName);
        }
        return clazz;
    }

    private static Method resolveMethod(Class<?> owner, String namedName, String intermediaryName, Class<?>... parameterTypes) throws NoSuchMethodException {
        try {
            Method method = owner.getMethod(namedName, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException ignored) {
        }
        Method method = owner.getMethod(intermediaryName, parameterTypes);
        method.setAccessible(true);
        return method;
    }

    private static Method resolveMethod(Class<?> owner, String namedName, String intermediaryName) throws NoSuchMethodException {
        return resolveMethod(owner, namedName, intermediaryName, new Class<?>[0]);
    }
}

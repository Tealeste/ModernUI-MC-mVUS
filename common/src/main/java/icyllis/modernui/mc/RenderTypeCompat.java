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

        private final Class<?> compositeStateClass;
        private final Method compositeStateBuilderFactory;
        private final Method compositeStateBuilderCreate;
        private final Method[] compositeStateBuilderSetters;

        private final Object lightmapStateShard;

        private final Constructor<?> textureStateShardCtor;
        private final Method renderTypeCreate;

        private CompositeStateImpl() {
            try {
                Class<?> renderTypeClass = requireClass(
                        "net.minecraft.client.renderer.RenderType",
                        "net.minecraft.class_1921"
                );

                renderTypeCreate = resolveRenderTypeCreate(renderTypeClass);
                compositeStateClass = renderTypeCreate.getParameterTypes()[5];

                compositeStateBuilderFactory = resolveCompositeStateBuilderFactory(compositeStateClass);
                Class<?> builderClass = compositeStateBuilderFactory.getReturnType();
                compositeStateBuilderSetters = resolveCompositeStateBuilderSetters(builderClass);
                compositeStateBuilderCreate = resolveCompositeStateBuilderCreate(builderClass, compositeStateClass);

                Class<?> renderStateShardClass = renderTypeClass.getSuperclass();
                lightmapStateShard = resolveLightmapStateShard(renderStateShardClass);
                textureStateShardCtor = resolveTextureStateShardConstructor(renderStateShardClass, compositeStateBuilderSetters);
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
                if (texture != null) {
                    invokeCompositeStateSetter(builder, textureState(texture));
                }
                if (lightmapEnabled) {
                    invokeCompositeStateSetter(builder, lightmapStateShard);
                }
                Object compositeState = compositeStateBuilderCreate.invoke(builder, /*outline*/ false);

                return renderTypeCreate.invoke(null, name, bufferSize, affectsCrumbling, sortOnUpload, renderPipeline, compositeState);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create RenderType via CompositeState", e);
            }
        }

        private Object textureState(Object texture) throws Exception {
            return textureStateShardCtor.newInstance(texture, /*mipmap*/ false);
        }

        private void invokeCompositeStateSetter(Object builder, Object stateShard) throws Exception {
            Method setter = resolveCompositeStateBuilderSetter(compositeStateBuilderSetters, stateShard.getClass());
            setter.invoke(builder, stateShard);
        }

        private static Method resolveRenderTypeCreate(Class<?> renderTypeClass) {
            for (Method method : renderTypeClass.getDeclaredMethods()) {
                if (!Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 6) {
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
                if (!renderTypeClass.isAssignableFrom(method.getReturnType())) {
                    continue;
                }
                method.setAccessible(true);
                return method;
            }
            throw new IllegalStateException("No compatible RenderType#create(String,int,boolean,boolean,RenderPipeline,CompositeState) overload found");
        }

        private static Method resolveCompositeStateBuilderFactory(Class<?> compositeStateClass) {
            for (Method method : compositeStateClass.getDeclaredMethods()) {
                if (!Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0) {
                    continue;
                }
                if (method.getReturnType() == void.class) {
                    continue;
                }
                method.setAccessible(true);
                return method;
            }
            throw new IllegalStateException("No compatible RenderType$CompositeState builder factory method found");
        }

        private static Method[] resolveCompositeStateBuilderSetters(Class<?> builderClass) {
            Method[] methods = builderClass.getDeclaredMethods();
            int count = 0;
            for (Method method : methods) {
                if (Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (method.getParameterCount() != 1 || method.getReturnType() != builderClass) {
                    continue;
                }
                method.setAccessible(true);
                count++;
            }
            Method[] setters = new Method[count];
            int i = 0;
            for (Method method : methods) {
                if (Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (method.getParameterCount() != 1 || method.getReturnType() != builderClass) {
                    continue;
                }
                setters[i++] = method;
            }
            return setters;
        }

        private static Method resolveCompositeStateBuilderCreate(Class<?> builderClass, Class<?> compositeStateClass) {
            for (Method method : builderClass.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1) {
                    continue;
                }
                if (method.getParameterTypes()[0] != boolean.class || method.getReturnType() != compositeStateClass) {
                    continue;
                }
                method.setAccessible(true);
                return method;
            }
            throw new IllegalStateException("No compatible CompositeStateBuilder#createCompositeState(boolean) method found");
        }

        private static Method resolveCompositeStateBuilderSetter(Method[] setters, Class<?> stateShardClass) {
            Method best = null;
            for (Method method : setters) {
                Class<?> param = method.getParameterTypes()[0];
                if (!param.isAssignableFrom(stateShardClass)) {
                    continue;
                }
                if (best == null) {
                    best = method;
                    continue;
                }
                Class<?> bestParam = best.getParameterTypes()[0];
                if (bestParam.isAssignableFrom(param)) {
                    best = method;
                }
            }
            if (best == null) {
                throw new IllegalStateException("No compatible CompositeStateBuilder setter method found for state shard: " + stateShardClass.getName());
            }
            return best;
        }

        private static Object resolveLightmapStateShard(Class<?> renderStateShardClass) throws Exception {
            for (Field field : renderStateShardClass.getFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (!renderStateShardClass.isAssignableFrom(field.getType())) {
                    continue;
                }
                Object value = field.get(null);
                if (value == null) {
                    continue;
                }
                String text = String.valueOf(value).toLowerCase();
                if (!text.contains("lightmap") || !text.contains("true")) {
                    continue;
                }
                return value;
            }
            throw new IllegalStateException("No compatible LIGHTMAP RenderStateShard found (enabled=true)");
        }

        private static Constructor<?> resolveTextureStateShardConstructor(Class<?> renderStateShardClass, Method[] setters) {
            Class<?> idClass = ResourceIdCompat.idClass();
            for (Class<?> nestedClass : renderStateShardClass.getDeclaredClasses()) {
                try {
                    Constructor<?> ctor = nestedClass.getDeclaredConstructor(idClass, boolean.class);
                    if (!isAcceptedByAnySetter(setters, nestedClass)) {
                        continue;
                    }
                    ctor.setAccessible(true);
                    return ctor;
                } catch (NoSuchMethodException ignored) {
                }
            }
            throw new IllegalStateException("No compatible TextureStateShard(ResourceId,boolean) constructor found");
        }

        private static boolean isAcceptedByAnySetter(Method[] setters, Class<?> stateShardClass) {
            for (Method setter : setters) {
                if (setter.getParameterTypes()[0].isAssignableFrom(stateShardClass)) {
                    return true;
                }
            }
            return false;
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

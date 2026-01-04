package icyllis.modernui.mc;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class MultiBufferSourceCompat {

    private static final Method END_BATCH_1 = resolveEndBatch1();
    private static final Method GET_BUFFER_1 = resolveGetBuffer1();

    private MultiBufferSourceCompat() {
    }

    public static void endBatch(MultiBufferSource.BufferSource bufferSource, Object renderType) {
        if (renderType == null) {
            return;
        }
        try {
            END_BATCH_1.invoke(bufferSource, renderType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call BufferSource#endBatch(renderType)", e);
        }
    }

    public static VertexConsumer getBuffer(MultiBufferSource source, Object renderType) {
        if (renderType == null) {
            throw new NullPointerException("renderType");
        }
        try {
            return (VertexConsumer) GET_BUFFER_1.invoke(source, renderType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call MultiBufferSource#getBuffer(renderType)", e);
        }
    }

    private static Method resolveEndBatch1() {
        for (Method method : MultiBufferSource.BufferSource.class.getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getReturnType() != void.class || method.getParameterCount() != 1) {
                continue;
            }
            if (method.getParameterTypes()[0].isPrimitive()) {
                continue;
            }
            if (method.getName().equals("toString")) {
                continue;
            }
            // BufferSource#endBatch(RenderType) (type moved/obfuscated across mappings)
            if (method.getParameterCount() == 1) {
                return method;
            }
        }
        throw new IllegalStateException("No compatible MultiBufferSource.BufferSource#endBatch(renderType) method found");
    }

    private static Method resolveGetBuffer1() {
        for (Method method : MultiBufferSource.class.getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getParameterCount() != 1) {
                continue;
            }
            if (!VertexConsumer.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            if (method.getParameterTypes()[0].isPrimitive()) {
                continue;
            }
            // MultiBufferSource#getBuffer(RenderType)
            if (method.getParameterCount() == 1) {
                return method;
            }
        }
        throw new IllegalStateException("No compatible MultiBufferSource#getBuffer(renderType) method found");
    }
}

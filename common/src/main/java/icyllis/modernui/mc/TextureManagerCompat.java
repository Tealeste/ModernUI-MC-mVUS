package icyllis.modernui.mc;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class TextureManagerCompat {

    private static final Method REGISTER = resolveRegister();
    private static final Method RELEASE = resolveRelease();

    private TextureManagerCompat() {
    }

    public static void register(TextureManager textureManager, Object id, AbstractTexture texture) {
        try {
            REGISTER.invoke(textureManager, id, texture);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register texture id=" + id, e);
        }
    }

    public static void release(TextureManager textureManager, Object id) {
        try {
            RELEASE.invoke(textureManager, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to release texture id=" + id, e);
        }
    }

    private static Method resolveRegister() {
        Class<?> idClass = ResourceIdCompat.idClass();
        for (Method method : TextureManager.class.getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getReturnType() != void.class || method.getParameterCount() != 2) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params[0] != idClass) {
                continue;
            }
            // Match the overload that can accept arbitrary AbstractTexture instances.
            // Do NOT match methods whose parameter is a subtype (e.g. ReloadableTexture),
            // otherwise reflective invocation can fail with "argument type mismatch".
            if (!params[1].isAssignableFrom(AbstractTexture.class)) {
                continue;
            }
            return method;
        }
        throw new IllegalStateException("No compatible TextureManager#register(id, texture) overload found");
    }

    private static Method resolveRelease() {
        Class<?> idClass = ResourceIdCompat.idClass();
        for (Method method : TextureManager.class.getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getReturnType() != void.class || method.getParameterCount() != 1) {
                continue;
            }
            if (method.getParameterTypes()[0] != idClass) {
                continue;
            }
            return method;
        }
        throw new IllegalStateException("No compatible TextureManager#release(id) overload found");
    }
}

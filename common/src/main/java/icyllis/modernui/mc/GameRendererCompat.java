package icyllis.modernui.mc;

import net.minecraft.client.renderer.GameRenderer;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Patch-level compatibility helpers for {@link GameRenderer} private API drift.
 */
public final class GameRendererCompat {

    private static final Method SET_POST_EFFECT = resolveSetPostEffect();

    private GameRendererCompat() {
    }

    public static void setPostEffect(GameRenderer renderer, Object effect) {
        try {
            SET_POST_EFFECT.invoke(renderer, effect);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke GameRenderer#setPostEffect", e);
        }
    }

    private static Method resolveSetPostEffect() {
        Class<?> idClass = ResourceIdCompat.idClass();
        for (Method method : GameRenderer.class.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getReturnType() != void.class || method.getParameterCount() != 1) {
                continue;
            }
            if (method.getParameterTypes()[0] != idClass) {
                continue;
            }
            method.setAccessible(true);
            return method;
        }
        throw new IllegalStateException("No compatible GameRenderer post-effect method found");
    }
}

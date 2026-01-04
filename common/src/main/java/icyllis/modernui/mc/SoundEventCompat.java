package icyllis.modernui.mc;

import net.minecraft.sounds.SoundEvent;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Patch-level compatibility helpers for {@link SoundEvent} factories whose parameter types drift
 * with {@code Identifier}/{@code ResourceLocation}.
 */
public final class SoundEventCompat {

    private static final Method CREATE_VARIABLE_RANGE_EVENT = resolveCreateVariableRangeEvent();

    private SoundEventCompat() {
    }

    public static SoundEvent createVariableRangeEvent(Object soundEventId) {
        try {
            return (SoundEvent) CREATE_VARIABLE_RANGE_EVENT.invoke(null, soundEventId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke SoundEvent#createVariableRangeEvent", e);
        }
    }

    private static Method resolveCreateVariableRangeEvent() {
        for (Method method : SoundEvent.class.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getParameterCount() != 1 || method.getReturnType() != SoundEvent.class) {
                continue;
            }
            return method;
        }
        for (Method method : SoundEvent.class.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getParameterCount() != 1 || method.getReturnType() != SoundEvent.class) {
                continue;
            }
            method.setAccessible(true);
            return method;
        }
        throw new IllegalStateException("No compatible SoundEvent variable-range factory found");
    }
}

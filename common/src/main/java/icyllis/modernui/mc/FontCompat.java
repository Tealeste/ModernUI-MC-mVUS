package icyllis.modernui.mc;

import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;

import java.lang.reflect.Modifier;
import java.lang.reflect.Method;
import java.util.Arrays;

public final class FontCompat {

    private static final Method PREPARE_TEXT_7 = resolvePrepareText(
            FormattedCharSequence.class,
            float.class, float.class,
            int.class,
            boolean.class,
            boolean.class,
            int.class
    );

    private static final Method PREPARE_TEXT_6 = resolvePrepareText(
            FormattedCharSequence.class,
            float.class, float.class,
            int.class,
            boolean.class,
            int.class
    );

    private FontCompat() {
    }

    public static Font.PreparedText prepareText(Font font,
                                                FormattedCharSequence text,
                                                float x,
                                                float y,
                                                int color,
                                                boolean dropShadow,
                                                boolean includeEmpty,
                                                int backgroundColor) {
        try {
            if (PREPARE_TEXT_7 != null) {
                return (Font.PreparedText) PREPARE_TEXT_7.invoke(
                        font,
                        text,
                        x, y,
                        color,
                        dropShadow,
                        includeEmpty,
                        backgroundColor
                );
            }
            if (PREPARE_TEXT_6 != null) {
                return (Font.PreparedText) PREPARE_TEXT_6.invoke(
                        font,
                        text,
                        x, y,
                        color,
                        dropShadow,
                        backgroundColor
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke Font#prepareText compat overload", e);
        }
        throw new IllegalStateException("No compatible Font#prepareText overload found");
    }

    private static Method resolvePrepareText(Class<?>... parameterTypes) {
        for (Method method : Font.class.getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getReturnType() != Font.PreparedText.class) {
                continue;
            }
            if (!Arrays.equals(method.getParameterTypes(), parameterTypes)) {
                continue;
            }
            method.setAccessible(true);
            return method;
        }
        for (Method method : Font.class.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getReturnType() != Font.PreparedText.class) {
                continue;
            }
            if (!Arrays.equals(method.getParameterTypes(), parameterTypes)) {
                continue;
            }
            method.setAccessible(true);
            return method;
        }
        return null;
    }
}

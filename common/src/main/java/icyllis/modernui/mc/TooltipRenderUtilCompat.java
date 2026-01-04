package icyllis.modernui.mc;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Patch-level compatibility helpers for {@code net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil}.
 */
public final class TooltipRenderUtilCompat {

    private static final Method RENDER_TOOLTIP_BACKGROUND = resolveRenderTooltipBackground();

    private TooltipRenderUtilCompat() {
    }

    public static void renderTooltipBackground(GuiGraphics graphics,
                                               int x,
                                               int y,
                                               int width,
                                               int height,
                                               Object tooltipStyle) {
        try {
            RENDER_TOOLTIP_BACKGROUND.invoke(null, graphics, x, y, width, height, tooltipStyle);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke TooltipRenderUtil#renderTooltipBackground", e);
        }
    }

    private static Method resolveRenderTooltipBackground() {
        for (Method method : TooltipRenderUtil.class.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getReturnType() != void.class || method.getParameterCount() != 6) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params[0] != GuiGraphics.class ||
                    params[1] != int.class ||
                    params[2] != int.class ||
                    params[3] != int.class ||
                    params[4] != int.class) {
                continue;
            }
            method.setAccessible(true);
            return method;
        }
        throw new IllegalStateException("No compatible TooltipRenderUtil tooltip background method found");
    }
}

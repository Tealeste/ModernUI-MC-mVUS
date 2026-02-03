/*
 * Modern UI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.mc.text.mixin;

import icyllis.modernui.mc.text.TextLayoutEngine;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.render.state.GuiTextRenderState;
import net.minecraft.network.chat.Style;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * 1.21.11+: Chat (and other UI text) hit-testing uses {@link ActiveTextCollector#findElementUnderCursor} which
 * transforms the screen point with {@link GuiTextRenderState#pose} and then walks active areas via
 * {@link net.minecraft.client.gui.Font.PreparedText#visit}.
 * <p>
 * Modern Text Engine may strip pose translation (and bake it into x/y) for rendering precision/performance, which
 * makes the vanilla active-area walk mismatch. Instead of relying on glyph active rectangles, use the ModernUI layout
 * engine's {@code styleAtWidth} to resolve the {@link Style} at the cursor position.
 */
@Mixin(ActiveTextCollector.class)
public interface MixinActiveTextCollector {

    @Inject(
            method = "findElementUnderCursor",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private static void modernUI_MC$findElementUnderCursor(GuiTextRenderState renderState,
                                                           float mouseX, float mouseY,
                                                           Consumer<Style> output,
                                                           CallbackInfo ci) {
        var scissor = renderState.scissor;
        if (scissor != null && !scissor.containsPoint((int) mouseX, (int) mouseY)) {
            ci.cancel();
            return;
        }

        // Convert to pose-local coords (vanilla semantics).
        Matrix3x2f inversePose = renderState.pose.invert(new Matrix3x2f());
        Vector2f localPoint = inversePose.transformPosition(new Vector2f(mouseX, mouseY));

        float localMouseX = localPoint.x();
        float localMouseY = localPoint.y();

        // Reject vertically in local coords; the splitter hit-test is 1D (x-only).
        int lineTop = renderState.y;
        int lineBottom = lineTop + renderState.font.lineHeight;
        if (localMouseY < lineTop || localMouseY >= lineBottom) {
            ci.cancel();
            return;
        }

        float width = localMouseX - renderState.x;
        Style style = TextLayoutEngine.getInstance()
                .getStringSplitter()
                .styleAtWidth(renderState.text, width);
        if (style != null) {
            output.accept(style);
        }

        ci.cancel();
    }
}


/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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

import icyllis.modernui.graphics.MathUtil;
import icyllis.modernui.mc.text.ModernPreparedText;
import icyllis.modernui.mc.text.ModernTextRenderer;
import icyllis.modernui.mc.text.TextLayout;
import icyllis.modernui.mc.text.TextLayoutEngine;
import icyllis.modernui.mc.text.TextRenderType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.GuiTextRenderState;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;

@Mixin(GuiTextRenderState.class)
public class MixinGuiTextRenderState {

    private static volatile VarHandle POSE_HANDLE;

    @Shadow
    private Font.PreparedText preparedText;

    private static VarHandle getPoseHandle() {
        VarHandle handle = POSE_HANDLE;
        if (handle != null) {
            return handle;
        }
        handle = resolvePoseHandle();
        POSE_HANDLE = handle;
        return handle;
    }

    private static VarHandle resolvePoseHandle() {
        try {
            Field field;
            try {
                // Mojang-named dev environment
                field = GuiTextRenderState.class.getDeclaredField("pose");
            } catch (NoSuchFieldException ignored) {
                // Production Fabric (intermediary): field names are obfuscated (field_*), so match by type.
                Field candidate = null;
                for (Field f : GuiTextRenderState.class.getDeclaredFields()) {
                    if ((f.getModifiers() & java.lang.reflect.Modifier.STATIC) != 0) {
                        continue;
                    }
                    if (Matrix3x2fc.class.isAssignableFrom(f.getType())) {
                        if (candidate != null) {
                            // Ambiguous: keep the old behavior of failing hard but with a clearer message.
                            throw new NoSuchFieldException("Multiple Matrix3x2fc-like fields found on GuiTextRenderState; cannot disambiguate");
                        }
                        candidate = f;
                    }
                }
                if (candidate == null) {
                    throw new NoSuchFieldException("No Matrix3x2fc-like field found on GuiTextRenderState");
                }
                field = candidate;
            }

            // Prefer a regular lookup (works for public fields without requiring module opens),
            // then fall back to a private lookup for private/internal fields.
            try {
                return MethodHandles.lookup().unreflectVarHandle(field);
            } catch (IllegalAccessException ignored) {
                var lookup = MethodHandles.privateLookupIn(GuiTextRenderState.class, MethodHandles.lookup());
                return lookup.unreflectVarHandle(field);
            }
        } catch (Throwable e) {
            // Fail soft: allow the game to boot even if internal layout details change.
            return null;
        }
    }

    private Matrix3x2fc poseOrNull() {
        VarHandle handle = getPoseHandle();
        if (handle == null) {
            return null;
        }
        return (Matrix3x2fc) handle.get((GuiTextRenderState) (Object) this);
    }

    @Redirect(method = "ensurePrepared",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;prepareText" +
                    "(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;"),
            require = 0)
    private Font.PreparedText onPrepareText(Font font, FormattedCharSequence text,
                                            float x, float y, int color, boolean dropShadow,
                                            boolean includeEmpty,
                                            int backgroundColor) {
        TextLayout layout = TextLayoutEngine.getInstance().lookupFormattedLayout(text);
        Matrix3x2fc ctm = poseOrNull();
        if (ctm == null) {
            // If we can't reliably read the pose matrix, preserve correctness by not stripping pose translation.
            return layout.prepareTextWithDensity(font, text, includeEmpty,
                    x, y,
                    color, dropShadow,
                    TextRenderType.MODE_NORMAL, /*uniformScale*/ 1.0f,
                    backgroundColor, /*stripPoseTranslation*/ false);
        }
        int mode;
        boolean isPureTranslation;
        if (!MathUtil.isApproxZero(ctm.m01()) ||
                !MathUtil.isApproxZero(ctm.m10()) ||
                !MathUtil.isApproxEqual(ctm.m00(), 1.0f) ||
                !MathUtil.isApproxEqual(ctm.m11(), 1.0f)) {
            isPureTranslation = false;
            if (ModernTextRenderer.sComputeDeviceFontSize &&
                    MathUtil.isApproxZero(ctm.m01()) &&
                    MathUtil.isApproxZero(ctm.m10()) &&
                    MathUtil.isApproxEqual(ctm.m00(), ctm.m11())) {
                mode = TextRenderType.MODE_UNIFORM_SCALE;
            } else if (ModernTextRenderer.sAllowSDFTextIn2D) {
                mode = TextRenderType.MODE_SDF_FILL;
            } else {
                mode = TextRenderType.MODE_NORMAL;
            }
        } else {
            mode = TextRenderType.MODE_NORMAL;
            isPureTranslation = true;
        }
        // compute exact font size and position
        float uniformScale = 1;
        boolean stripPoseTranslation = false;
        if (ModernTextRenderer.sComputeDeviceFontSize &&
                (isPureTranslation || mode == TextRenderType.MODE_UNIFORM_SCALE)) {
            // uniform scale case
            // extract the translation vector for snapping to pixel grid
            x += ctm.m20() / ctm.m00();
            y += ctm.m21() / ctm.m11();
            stripPoseTranslation = true;
            // total scale
            uniformScale = ctm.m00();
            if (MathUtil.isApproxEqual(uniformScale, 1)) {
                mode = TextRenderType.MODE_NORMAL;
            } else {
                float upperLimit = Math.max(1.0f,
                        (float) TextLayoutEngine.sMinPixelDensityForSDF / layout.getCreatedResLevel());
                if (uniformScale <= upperLimit) {
                    // uniform scale smaller and not too large
                    mode = TextRenderType.MODE_UNIFORM_SCALE;
                } else {
                    mode = ModernTextRenderer.sAllowSDFTextIn2D ? TextRenderType.MODE_SDF_FILL : TextRenderType.MODE_NORMAL;
                }
            }
        }
        return layout.prepareTextWithDensity(font, text, includeEmpty,
                x, y,
                color, dropShadow,
                mode, uniformScale,
                backgroundColor, stripPoseTranslation);
    }

    @Redirect(method = "ensurePrepared",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;prepareText" +
                    "(Lnet/minecraft/util/FormattedCharSequence;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;"),
            require = 0)
    private Font.PreparedText onPrepareTextLegacy(Font font, FormattedCharSequence text,
                                                  float x, float y, int color, boolean dropShadow,
                                                  int backgroundColor) {
        // 1.21.10 and older do not have the 'includeEmpty' parameter.
        return onPrepareText(font, text, x, y, color, dropShadow, /*includeEmpty*/ false, backgroundColor);
    }

    @Redirect(method = "ensurePrepared",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/navigation/ScreenRectangle;" +
                    "transformMaxBounds(Lorg/joml/Matrix3x2f;)Lnet/minecraft/client/gui/navigation/ScreenRectangle;"),
            require = 0)
    private ScreenRectangle onTransformMaxBounds(ScreenRectangle bounds, Matrix3x2f pose) {
        Font.PreparedText preparedText = this.preparedText;
        if (preparedText instanceof ModernPreparedText modernPreparedText &&
                modernPreparedText.isStripPoseTranslation()) {
            Matrix3x2f poseNoTranslation = new Matrix3x2f(pose);
            poseNoTranslation.m20 = 0;
            poseNoTranslation.m21 = 0;
            return bounds.transformMaxBounds(poseNoTranslation);
        }
        return bounds.transformMaxBounds(pose);
    }

    @Redirect(method = "ensurePrepared",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/navigation/ScreenRectangle;" +
                    "transformMaxBounds(Lorg/joml/Matrix3x2fc;)Lnet/minecraft/client/gui/navigation/ScreenRectangle;"),
            require = 0)
    private ScreenRectangle onTransformMaxBoundsModern(ScreenRectangle bounds, Matrix3x2fc pose) {
        Font.PreparedText preparedText = this.preparedText;
        if (preparedText instanceof ModernPreparedText modernPreparedText &&
                modernPreparedText.isStripPoseTranslation()) {
            Matrix3x2f poseNoTranslation = new Matrix3x2f(pose);
            poseNoTranslation.m20 = 0;
            poseNoTranslation.m21 = 0;
            return bounds.transformMaxBounds(poseNoTranslation);
        }
        return bounds.transformMaxBounds(pose);
    }
}

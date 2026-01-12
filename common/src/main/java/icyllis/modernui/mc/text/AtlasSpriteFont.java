/*
 * Modern UI.
 * Copyright (C) 2019-2025 BloCamLimb. All rights reserved.
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

package icyllis.modernui.mc.text;

import com.mojang.blaze3d.textures.GpuTextureView;
import icyllis.arc3d.sketch.Typeface;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.graphics.text.Font;
import icyllis.modernui.graphics.text.FontMetricsInt;
import icyllis.modernui.graphics.text.FontPaint;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Objects;

public final class AtlasSpriteFont implements Font {

    public static final int PLACEHOLDER_CODE_POINT = 0xFFFC;

    private final FontDescription.AtlasSprite sprite;

    public AtlasSpriteFont(@Nonnull FontDescription.AtlasSprite sprite) {
        this.sprite = sprite;
    }

    @Nonnull
    public FontDescription.AtlasSprite sprite() {
        return sprite;
    }

    @Nonnull
    public ResourceLocation atlasId() {
        return sprite.atlasId();
    }

    @Nonnull
    public ResourceLocation spriteId() {
        return sprite.spriteId();
    }

    @Nonnull
    public GpuTextureView getTextureView() {
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(atlasId());
        return atlas.getTextureView();
    }

    @Override
    public int getStyle() {
        return FontPaint.NORMAL;
    }

    @Override
    public String getFullName(@Nonnull Locale locale) {
        return "atlas_sprite:" + sprite;
    }

    @Override
    public String getFamilyName(@Nonnull Locale locale) {
        return "atlas_sprite";
    }

    @Override
    public int getMetrics(@Nonnull FontPaint paint, FontMetricsInt fm) {
        return 0;
    }

    @Override
    public boolean hasGlyph(int ch, int vs) {
        return ch == PLACEHOLDER_CODE_POINT;
    }

    @Override
    public float doSimpleLayout(char[] buf, int start, int limit, FontPaint paint, IntArrayList glyphs,
                                FloatArrayList positions, float x, float y) {
        return doComplexLayout(buf, start, limit, start, limit, false, paint, glyphs, positions, null, 0, null, x, y);
    }

    @Override
    public float doComplexLayout(char[] buf, int contextStart, int contextLimit, int layoutStart, int layoutLimit,
                                 boolean isRtl, FontPaint paint, IntArrayList glyphs, FloatArrayList positions,
                                 float[] advances, int advanceOffset, Rect bounds, float x, float y) {
        float scaleUp = (int) (paint.getFontSize() / TextLayoutProcessor.sBaseFontSize + 0.5);
        float adv = TextLayoutProcessor.DEFAULT_BASE_FONT_SIZE * scaleUp;

        float advance = 0;
        for (int i = 0; i < layoutLimit - layoutStart; i++) {
            int index = isRtl ? (layoutLimit - 1 - i) : (layoutStart + i);
            if (buf[index] != PLACEHOLDER_CODE_POINT) {
                continue;
            }
            if (advances != null) {
                advances[index - advanceOffset] = adv;
            }
            if (glyphs != null) {
                glyphs.add(PLACEHOLDER_CODE_POINT);
            }
            if (positions != null) {
                positions.add(x + advance);
                positions.add(y);
            }
            advance += adv;
        }
        return advance;
    }

    @Nullable
    @Override
    public Typeface getNativeTypeface() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AtlasSpriteFont that)) return false;
        return Objects.equals(sprite, that.sprite);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sprite);
    }
}

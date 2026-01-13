/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.textures.GpuTextureView;
import icyllis.modernui.graphics.text.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.*;
import net.minecraft.client.gui.font.glyphs.*;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Style;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * This class is used only for <b>compatibility</b>.
 * <p>
 * Some mods have it own {@link net.minecraft.client.gui.Font.StringRenderOutput},
 * we have to provide per-code-point glyph info. Minecraft vanilla maps code points
 * to glyphs without text shaping (no international support). It also ignores
 * resolution level (GUI scale), we assume it's current and round it up. Vanilla
 * doesn't support FreeType embolden as well, we ignore it.
 * <p>
 * This class is similar to {@link FontCollection} but no font itemization.
 * <p>
 * We use our own font atlas and rectangle packing algorithm.
 *
 * @author BloCamLimb
 * @since 3.8
 */
@SuppressWarnings("JavadocReference")
public class StandardFontSet extends FontSet {

    @Unmodifiable
    private List<FontFamily> mFamilies = Collections.emptyList();

    private CodepointMap<BakedGlyph> mGlyphs;

    private final IntFunction<BakedGlyph> mCacheGlyph = this::cacheGlyph;

    private final IdentityHashMap<GpuTextureView, GlyphRenderTypes> mGlyphRenderTypesBitmap = new IdentityHashMap<>();

    private final IdentityHashMap<GpuTextureView, GlyphRenderTypes> mGlyphRenderTypesNormal = new IdentityHashMap<>();

    private final GlyphSource mGlyphSource = new GlyphSource() {
        @Override
        public BakedGlyph getGlyph(int codePoint) {
            return getCachedGlyph(codePoint);
        }

        @Override
        public BakedGlyph getRandomGlyph(RandomSource random, int codePoint) {
            return getCachedGlyph(codePoint);
        }
    };

    private float mResLevel = 2;
    private final FontPaint mStandardPaint = new FontPaint();

    public StandardFontSet(@Nonnull TextureManager texMgr,
                           @Nonnull Object fontName) {
        super(new GlyphStitcher(texMgr, Minecraft.UNIFORM_FONT)); // <- unused

        mStandardPaint.setFontStyle(FontPaint.NORMAL);
        mStandardPaint.setLocale(Locale.ROOT);
    }

    public void reload(@Nonnull FontCollection fontCollection, int newResLevel) {
        super.reload(Collections.emptyList(), Collections.emptySet());
        mFamilies = fontCollection.getFamilies();
        invalidateCache(newResLevel);
    }

    public void invalidateCache(int newResLevel) {
        if (mGlyphs != null) {
            mGlyphs.clear();
        }
        mGlyphRenderTypesBitmap.clear();
        mGlyphRenderTypesNormal.clear();
        int fontSize = TextLayoutProcessor.computeFontSize(newResLevel);
        mStandardPaint.setFontSize(fontSize);
        mStandardPaint.setAntiAlias(GlyphManager.sAntiAliasing);
        mStandardPaint.setLinearMetrics(GlyphManager.sFractionalMetrics);
        mResLevel = newResLevel;
    }

    @Override
    public GlyphSource source(boolean notFishyGlyphs) {
        return mGlyphSource;
    }

    @Nonnull
    private BakedGlyph getCachedGlyph(int codePoint) {
        if (mGlyphs == null) {
            mGlyphs = new CodepointMap<>(BakedGlyph[]::new, BakedGlyph[][]::new);
        }
        return mGlyphs.computeIfAbsent(codePoint, mCacheGlyph);
    }

    @Nonnull
    private GlyphRenderTypes getGlyphRenderTypes(@Nonnull GpuTextureView textureView, boolean isBitmap) {
        final var map = isBitmap ? mGlyphRenderTypesBitmap : mGlyphRenderTypesNormal;
        return map.computeIfAbsent(textureView, view -> new GlyphRenderTypes(
                TextRenderType.getOrCreate(view, net.minecraft.client.gui.Font.DisplayMode.NORMAL, isBitmap),
                TextRenderType.getOrCreate(view, net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH, isBitmap),
                TextRenderType.getOrCreate(view, net.minecraft.client.gui.Font.DisplayMode.POLYGON_OFFSET, isBitmap),
                TextRenderType.getPipelineForGui(TextRenderType.MODE_NORMAL, isBitmap)
        ));
    }

    @Nonnull
    private static BakedGlyph bakeEmpty(float advance) {
        GlyphInfo info = new StandardGlyphInfo(advance);
        return new BakedGlyph() {
            @Override
            public GlyphInfo info() {
                return info;
            }

            @Override
            public net.minecraft.client.gui.font.TextRenderable createGlyph(float x, float y, int color,
                                                                            int packedLight, Style style,
                                                                            float r, float g) {
                return null;
            }
        };
    }

    @Nonnull
    private BakedGlyph bakeGlyph(@Nonnull GlyphInfo info,
                                 @Nonnull GpuTextureView textureView,
                                 boolean isBitmap,
                                 float u0, float u1, float v0, float v1,
                                 float left, float right, float up, float down) {
        return new BakedSheetGlyph(
                info,
                getGlyphRenderTypes(textureView, isBitmap),
                textureView,
                u0, u1, v0, v1,
                left, right, up, down
        );
    }

    @Nonnull
    private BakedGlyph cacheGlyph(int codePoint) {
        for (FontFamily family : mFamilies) {
            if (!family.hasGlyph(codePoint)) {
                continue;
            }
            Font font = family.getClosestMatch(FontPaint.NORMAL);
            // we MUST check BitmapFont first,
            // because codePoint may be an invalid Unicode code point
            // but vanilla doesn't validate that
            if (font instanceof BitmapFont bitmapFont) {
                var glyphInfo = bitmapFont.getGlyph(codePoint);
                if (glyphInfo == null) {
                    return super.source(false).getGlyph(codePoint);
                }
                float advance = glyphInfo.getAdvance() / TextLayoutEngine.BITMAP_SCALE;
                var glyph = GlyphManager.getInstance().lookupGlyph(
                        bitmapFont,
                        (int) mStandardPaint.getFontSize(),
                        codePoint
                );
                var textureView = GlyphManager.getInstance().getCurrentTexture(bitmapFont);
                if (glyph != null) {
                    float up = TextLayout.STANDARD_BASELINE_OFFSET +
                            (float) glyph.y / TextLayoutEngine.BITMAP_SCALE;
                    float left = (float) glyph.x / TextLayoutEngine.BITMAP_SCALE;
                    float right = left + (float) glyph.width / TextLayoutEngine.BITMAP_SCALE;
                    float down = up + (float) glyph.height / TextLayoutEngine.BITMAP_SCALE;
                    if (textureView == null) {
                        return super.source(false).getGlyph(codePoint);
                    }
                    return bakeGlyph(new StandardGlyphInfo(advance), textureView, /*isBitmap*/ true,
                            glyph.u1, glyph.u2, glyph.v1, glyph.v2,
                            left, right, up, down);
                }
                return bakeEmpty(advance);
            } else if (font instanceof SpaceFont spaceFont) {
                float adv = spaceFont.getAdvance(codePoint);
                if (!Float.isNaN(adv) && adv > 0) {
                    return bakeEmpty(adv);
                }
                return super.source(false).getGlyph(codePoint);
            } else if (font instanceof OutlineFont outlineFont) {
                char[] chars = Character.toChars(codePoint);
                IntArrayList glyphs = new IntArrayList(1);
                float adv = outlineFont.doSimpleLayout(
                        chars,
                        0, chars.length,
                        mStandardPaint, glyphs, null,
                        0, 0
                );
                float advance = adv / mResLevel;
                if (glyphs.size() == 1 && glyphs.getInt(0) != 0) { // 0 is the missing glyph for TTF
                    var glyph = GlyphManager.getInstance().lookupGlyph(
                            outlineFont,
                            (int) mStandardPaint.getFontSize(),
                            glyphs.getInt(0)
                    );
                    if (glyph != null) {
                        float up = TextLayout.STANDARD_BASELINE_OFFSET +
                                (float) glyph.y / mResLevel;
                        float left = (float) glyph.x / mResLevel;
                        float right = left + (float) glyph.width / mResLevel;
                        float down = up + (float) glyph.height / mResLevel;
                        GpuTextureView fontTexture = GlyphManager.getInstance().getFontTexture();
                        if (fontTexture == null) {
                            return super.source(false).getGlyph(codePoint);
                        }
                        return bakeGlyph(new StandardGlyphInfo(advance), fontTexture,
                                /*isBitmap*/ false,
                                glyph.u1, glyph.u2, glyph.v1, glyph.v2,
                                left, right, up, down);
                    }
                }
                if (adv > 0) {
                    return bakeEmpty(advance);
                }
                return super.source(false).getGlyph(codePoint);
            }
            // color emoji requires complex layout, so no support
        }
        return super.source(false).getGlyph(codePoint);
    }

    // no obfuscated support

    public static class StandardGlyphInfo implements GlyphInfo {

        private final float mAdvance;

        public StandardGlyphInfo(float advance) {
            mAdvance = advance;
        }

        @Override
        public float getAdvance() {
            return mAdvance;
        }

        @Override
        public float getBoldOffset() {
            return 0.5f;
        }

        @Override
        public float getShadowOffset() {
            return ModernTextRenderer.sShadowOffset;
        }
    }
}

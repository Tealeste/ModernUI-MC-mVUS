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

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.*;
import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.ImmediateContext;
import icyllis.arc3d.engine.SamplerDesc;
import icyllis.arc3d.opengl.GLCaps;
import icyllis.arc3d.opengl.GLSampler;
import icyllis.modernui.core.Core;
import icyllis.modernui.mc.ModernUIMod;
import icyllis.modernui.mc.RenderPipelineCompat;
import icyllis.modernui.mc.MuiModApi;
import icyllis.modernui.mc.SamplerCompat;
import icyllis.modernui.mc.TextureManagerCompat;
import icyllis.modernui.mc.text.mixin.AccessBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.function.Supplier;

import static icyllis.modernui.mc.ModernUIMod.LOGGER;
import static icyllis.modernui.mc.text.TextLayoutEngine.MARKER;

/**
 * Fast and modern text render type.
 */
public final class TextRenderType {

    public static final int MODE_NORMAL = 0; // <- must be zero
    public static final int MODE_SDF_FILL = 1;
    public static final int MODE_SDF_STROKE = 2;
    public static final int MODE_SEE_THROUGH = 3;
    /**
     * Used in 2D rendering, render as {@link #MODE_NORMAL},
     * but we compute font size in device space from CTM.
     *
     * @since 3.8.1
     */
    public static final int MODE_UNIFORM_SCALE = 4; // <- must be power of 2

    public static final RenderPipeline PIPELINE_NORMAL = withFragmentShader(
            withLocation(RenderPipeline.builder(), "pipeline/modern_text_normal")
                    .withVertexShader("core/rendertype_text_intensity"),
            "core/rendertype_modern_text_normal")
            .withUniform("Fog", UniformType.UNIFORM_BUFFER)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS)
            .build();

    public static final RenderPipeline.Snippet PIPELINE_SDF_SNIPPET = RenderPipeline.builder()
            .withVertexShader("core/rendertype_text_intensity")
            .withUniform("Fog", UniformType.UNIFORM_BUFFER)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS)
            .buildSnippet();

    public static final RenderPipeline PIPELINE_SDF_FILL = withFragmentShader(
            withLocation(RenderPipeline.builder(PIPELINE_SDF_SNIPPET), "pipeline/modern_text_sdf_fill"),
            "core/rendertype_modern_text_sdf_fill")
            .withDepthBias(-1.0F, -10.0F)
            .build();

    public static final RenderPipeline PIPELINE_SDF_STROKE = withFragmentShader(
            withLocation(RenderPipeline.builder(PIPELINE_SDF_SNIPPET), "pipeline/modern_text_sdf_stroke"),
            "core/rendertype_modern_text_sdf_stroke")
            .withDepthBias(-1.0F, -10.0F)
            .build();

    private static volatile RenderPipeline sCurrentPipelineSDFFill = PIPELINE_SDF_FILL;
    private static volatile RenderPipeline sCurrentPipelineSDFStroke = PIPELINE_SDF_STROKE;

    // Hey buddy, I think you've got the wrong door, the leather club's two blocks down
    public static final RenderPipeline PIPELINE_SDF_FILL_SMART = withFragmentShader(
            withLocation(RenderPipeline.builder(PIPELINE_SDF_SNIPPET), "pipeline/modern_text_sdf_fill_smart"),
            "core/rendertype_modern_text_sdf_fill_400")
            .withDepthBias(-1.0F, -10.0F)
            .build();

    public static final RenderPipeline PIPELINE_SDF_STROKE_SMART = withFragmentShader(
            withLocation(RenderPipeline.builder(PIPELINE_SDF_SNIPPET), "pipeline/modern_text_sdf_stroke_smart"),
            "core/rendertype_modern_text_sdf_stroke_400")
            .withDepthBias(-1.0F, -10.0F)
            .build();

    /*public static final ShaderProgram SHADER_NORMAL = new ShaderProgram(
            ModernUIMod.location("core/rendertype_modern_text_normal"),
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
            ShaderDefines.EMPTY
    );

    public static final ShaderProgram SHADER_SDF_FILL = new ShaderProgram(
            ModernUIMod.location("core/rendertype_modern_text_sdf_fill"),
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
            ShaderDefines.EMPTY
    );
    public static final ShaderProgram SHADER_SDF_STROKE = new ShaderProgram(
            ModernUIMod.location("core/rendertype_modern_text_sdf_stroke"),
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
            ShaderDefines.EMPTY
    );

    private static volatile ShaderProgram sCurrentShaderSDFFill = SHADER_SDF_FILL;
    private static volatile ShaderProgram sCurrentShaderSDFStroke = SHADER_SDF_STROKE;

    private static final ShaderProgram SHADER_SDF_FILL_SMART = new ShaderProgram(
            ModernUIMod.location("core/rendertype_modern_text_sdf_fill_400"),
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
            ShaderDefines.EMPTY
    );
    private static final ShaderProgram SHADER_SDF_STROKE_SMART = new ShaderProgram(
            ModernUIMod.location("core/rendertype_modern_text_sdf_stroke_400"),
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
            ShaderDefines.EMPTY
    );*/

    /*static final ShaderStateShard
            RENDERTYPE_MODERN_TEXT_NORMAL = new ShaderStateShard(TextRenderType::getShaderNormal),
            RENDERTYPE_MODERN_TEXT_SDF_FILL = new ShaderStateShard(TextRenderType::getShaderSDFFill),
            RENDERTYPE_MODERN_TEXT_SDF_STROKE = new ShaderStateShard(TextRenderType::getShaderSDFStroke);*/

    private static RenderPipeline.Builder withLocation(RenderPipeline.Builder builder, String path) {
        return RenderPipelineCompat.withLocation(builder, ModernUIMod.location(path));
    }

    private static RenderPipeline.Builder withFragmentShader(RenderPipeline.Builder builder, String path) {
        return RenderPipelineCompat.withFragmentShader(builder, ModernUIMod.location(path));
    }

    /*
     * Only the texture id is different, the rest state are same
     */
    /*private static final ImmutableList<RenderStateShard> NORMAL_STATES;
    private static final ImmutableList<RenderStateShard> SDF_FILL_STATES;
    private static final ImmutableList<RenderStateShard> SDF_STROKE_STATES;
    static final ImmutableList<RenderStateShard> VANILLA_STATES;
    static final ImmutableList<RenderStateShard> SEE_THROUGH_STATES;
    static final ImmutableList<RenderStateShard> POLYGON_OFFSET_STATES;*/

    /**
     * Texture id to render type map
     */
    private static final HashMap<GpuTextureView, Object> sNormalTypes = new HashMap<>();
    private static final HashMap<GpuTextureView, Object> sSDFFillTypes = new HashMap<>();
    private static final HashMap<GpuTextureView, Object> sSDFStrokeTypes = new HashMap<>();
    private static final HashMap<GpuTextureView, Object> sVanillaTypes = new HashMap<>();
    private static final HashMap<GpuTextureView, Object> sSeeThroughTypes = new HashMap<>();
    private static final HashMap<GpuTextureView, Object> sPolygonOffsetTypes = new HashMap<>();

    private static Object sFirstSDFFillType;
    private static final ByteBufferBuilder sFirstSDFFillBuffer = new ByteBufferBuilder(131072);

    private static Object sFirstSDFStrokeType;
    private static final ByteBufferBuilder sFirstSDFStrokeBuffer = new ByteBufferBuilder(131072);

    // SDF requires bilinear sampling
    //@SharedPtr
    //private static GLSampler sLinearFontSampler;

    static {
        /*NORMAL_STATES = ImmutableList.of(
                //RENDERTYPE_MODERN_TEXT_NORMAL,
                TRANSLUCENT_TRANSPARENCY,
                LEQUAL_DEPTH_TEST,
                CULL,
                LIGHTMAP,
                NO_OVERLAY,
                NO_LAYERING,
                MAIN_TARGET,
                DEFAULT_TEXTURING,
                COLOR_DEPTH_WRITE,
                DEFAULT_LINE
        );*/
        /*SDF_FILL_STATES = ImmutableList.of(
                //RENDERTYPE_MODERN_TEXT_SDF_FILL,
                TRANSLUCENT_TRANSPARENCY,
                LEQUAL_DEPTH_TEST,
                CULL,
                LIGHTMAP,
                NO_OVERLAY,
                POLYGON_OFFSET_LAYERING,
                MAIN_TARGET,
                DEFAULT_TEXTURING,
                COLOR_DEPTH_WRITE,
                DEFAULT_LINE
        );*/
        /*SDF_STROKE_STATES = ImmutableList.of(
                //RENDERTYPE_MODERN_TEXT_SDF_STROKE,
                TRANSLUCENT_TRANSPARENCY,
                LEQUAL_DEPTH_TEST,
                CULL,
                LIGHTMAP,
                NO_OVERLAY,
                POLYGON_OFFSET_LAYERING,
                MAIN_TARGET,
                DEFAULT_TEXTURING,
                COLOR_DEPTH_WRITE,
                DEFAULT_LINE
        );*/
        /*VANILLA_STATES = ImmutableList.of(
                RENDERTYPE_TEXT_SHADER,
                TRANSLUCENT_TRANSPARENCY,
                LEQUAL_DEPTH_TEST,
                CULL,
                LIGHTMAP,
                NO_OVERLAY,
                NO_LAYERING,
                MAIN_TARGET,
                DEFAULT_TEXTURING,
                COLOR_DEPTH_WRITE,
                DEFAULT_LINE
        );*/
        /*SEE_THROUGH_STATES = ImmutableList.of(
                RENDERTYPE_TEXT_SEE_THROUGH_SHADER,
                TRANSLUCENT_TRANSPARENCY,
                NO_DEPTH_TEST,
                CULL,
                LIGHTMAP,
                NO_OVERLAY,
                NO_LAYERING,
                MAIN_TARGET,
                DEFAULT_TEXTURING,
                COLOR_WRITE,
                DEFAULT_LINE
        );*/
        /*POLYGON_OFFSET_STATES = ImmutableList.of(
                RENDERTYPE_TEXT_SHADER,
                TRANSLUCENT_TRANSPARENCY,
                LEQUAL_DEPTH_TEST,
                CULL,
                LIGHTMAP,
                NO_OVERLAY,
                POLYGON_OFFSET_LAYERING,
                MAIN_TARGET,
                DEFAULT_TEXTURING,
                COLOR_DEPTH_WRITE,
                DEFAULT_LINE
        );*/
    }

    private static final Supplier<Object> DEFAULT_SAMPLER =
            () -> SamplerCompat.clampToEdge(FilterMode.NEAREST);
    private static final Supplier<Object> SDF_SAMPLER =
            () -> SamplerCompat.clampToEdge(FilterMode.LINEAR, true);

    @Nonnull
    static Object getSamplerForGui(int mode) {
        return switch (mode) {
            case MODE_SDF_FILL, MODE_SDF_STROKE -> SDF_SAMPLER.get();
            default -> DEFAULT_SAMPLER.get();
        };
    }

    private static final IdentityHashMap<GpuTextureView, Object> sTextureIds = new IdentityHashMap<>();
    private static int sNextTextureId = 0;

    private TextRenderType() {
    }

    private static synchronized Object getOrRegisterTextureId(GpuTextureView texture) {
        Object id = sTextureIds.get(texture);
        if (id != null) {
            return id;
        }
        id = ModernUIMod.location("internal/text_texture/" + (sNextTextureId++));
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        TextureManagerCompat.register(textureManager, id, new TextureViewBackedTexture(texture));
        sTextureIds.put(texture, id);
        return id;
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static <RT> RT getOrCreate(GpuTextureView texture, int mode) {
        return (RT) switch (mode) {
            case MODE_SDF_FILL -> {
                if (!TextLayoutEngine.sCurrentInWorldRendering || TextLayoutEngine.sUseTextShadersInWorld) {
                    yield sSDFFillTypes.computeIfAbsent(texture, TextRenderType::makeSDFFillType);
                } else {
                    yield sPolygonOffsetTypes.computeIfAbsent(texture, TextRenderType::makePolygonOffsetType);
                }
            }
            case MODE_SDF_STROKE -> sSDFStrokeTypes.computeIfAbsent(texture, TextRenderType::makeSDFStrokeType);
            case MODE_SEE_THROUGH -> sSeeThroughTypes.computeIfAbsent(texture, TextRenderType::makeSeeThroughType);
            default -> {
                if (!TextLayoutEngine.sCurrentInWorldRendering || TextLayoutEngine.sUseTextShadersInWorld) {
                    yield sNormalTypes.computeIfAbsent(texture, TextRenderType::makeNormalType);
                } else {
                    yield sVanillaTypes.computeIfAbsent(texture, TextRenderType::makeVanillaType);
                }
            }
        };
    }

    // compatibility
    @Nonnull
    @SuppressWarnings("unchecked")
    public static <RT> RT getOrCreate(GpuTextureView texture, Font.DisplayMode mode, boolean isBitmapFont) {
        return (RT) switch (mode) {
            case SEE_THROUGH -> sSeeThroughTypes.computeIfAbsent(texture, TextRenderType::makeSeeThroughType);
            case POLYGON_OFFSET -> sPolygonOffsetTypes.computeIfAbsent(texture, TextRenderType::makePolygonOffsetType);
            default -> isBitmapFont || (TextLayoutEngine.sCurrentInWorldRendering && !TextLayoutEngine.sUseTextShadersInWorld)
                    ? sVanillaTypes.computeIfAbsent(texture, TextRenderType::makeVanillaType)
                    : sNormalTypes.computeIfAbsent(texture, TextRenderType::makeNormalType);
        };
    }

    public static RenderPipeline getPipelineForGui(int mode, boolean isBitmapFont) {
        return switch (mode) {
            case MODE_SDF_FILL -> sCurrentPipelineSDFFill;
            default -> isBitmapFont
                    ? RenderPipelines.TEXT
                    : PIPELINE_NORMAL;
        };
    }

    @Nonnull
    private static Object makeNormalType(GpuTextureView texture) {
        Supplier<Object> sampler = SamplerCompat.isSupported() ? DEFAULT_SAMPLER : null;
        return MuiModApi.get().createRenderType("modern_text_normal", 256,
                false, true, PIPELINE_NORMAL,
                getOrRegisterTextureId(texture),
                sampler,
                true);
    }

    /*private static void ensureLinearFontSampler() {
        if (sLinearFontSampler == null) {
            ImmediateContext context = Core.requireImmediateContext();
            // default state is bilinear
            sLinearFontSampler = (GLSampler) context.getResourceProvider().findOrCreateCompatibleSampler(
                    SamplerDesc.make(SamplerDesc.FILTER_LINEAR, SamplerDesc.MIPMAP_MODE_LINEAR));
            Objects.requireNonNull(sLinearFontSampler, "Failed to create sampler object");
        }
    }*/

    @Nonnull
    private static Object makeSDFFillType(GpuTextureView texture) {
        /*ensureLinearFontSampler();
        TextRenderType renderType = new TextRenderType("modern_text_sdf_fill", 256, () -> {
            //RenderSystem.setShader(getShaderSDFFill());
            SDF_FILL_STATES.forEach(RenderStateShard::setupRenderState);
            //RenderSystem.setShaderTexture(0, texture);
            if (!TextLayoutEngine.sCurrentInWorldRendering || TextLayoutEngine.sUseTextShadersInWorld) {
                GL33C.glBindSampler(0, sLinearFontSampler.getHandle());
            }
        }, () -> {
            SDF_FILL_STATES.forEach(RenderStateShard::clearRenderState);
            if (!TextLayoutEngine.sCurrentInWorldRendering || TextLayoutEngine.sUseTextShadersInWorld) {
                GL33C.glBindSampler(0, 0);
            }
        });*/
        Object renderType = MuiModApi.get().createRenderType("modern_text_sdf_fill", 256,
                false, true, sCurrentPipelineSDFFill,
                getOrRegisterTextureId(texture),
                SDF_SAMPLER,
                true);
        if (sFirstSDFFillType == null) {
            assert (sSDFFillTypes.isEmpty());
            sFirstSDFFillType = renderType;
            if (TextLayoutEngine.sUseTextShadersInWorld) {
                try {
                    ((AccessBufferSource) Minecraft.getInstance().renderBuffers().bufferSource()).getFixedBuffers()
                            .put(renderType, sFirstSDFFillBuffer);
                } catch (Exception e) {
                    LOGGER.warn(MARKER, "Failed to add SDF fill to fixed buffers", e);
                }
            }
        }
        return renderType;
    }

    @Nonnull
    private static Object makeSDFStrokeType(GpuTextureView texture) {
        /*ensureLinearFontSampler();
        TextRenderType renderType = new TextRenderType("modern_text_sdf_stroke", 256, () -> {
            //RenderSystem.setShader(getShaderSDFStroke());
            SDF_STROKE_STATES.forEach(RenderStateShard::setupRenderState);
            //RenderSystem.setShaderTexture(0, texture);
            if (!TextLayoutEngine.sCurrentInWorldRendering || TextLayoutEngine.sUseTextShadersInWorld) {
                GL33C.glBindSampler(0, sLinearFontSampler.getHandle());
            }
        }, () -> {
            SDF_STROKE_STATES.forEach(RenderStateShard::clearRenderState);
            if (!TextLayoutEngine.sCurrentInWorldRendering || TextLayoutEngine.sUseTextShadersInWorld) {
                GL33C.glBindSampler(0, 0);
            }
        });*/
        Object renderType = MuiModApi.get().createRenderType("modern_text_sdf_stroke", 256,
                false, true, sCurrentPipelineSDFStroke,
                getOrRegisterTextureId(texture),
                SDF_SAMPLER,
                true);
        if (sFirstSDFStrokeType == null) {
            assert (sSDFStrokeTypes.isEmpty());
            sFirstSDFStrokeType = renderType;
            if (TextLayoutEngine.sUseTextShadersInWorld) {
                try {
                    ((AccessBufferSource) Minecraft.getInstance().renderBuffers().bufferSource()).getFixedBuffers()
                            .put(renderType, sFirstSDFStrokeBuffer);
                } catch (Exception e) {
                    LOGGER.warn(MARKER, "Failed to add SDF stroke to fixed buffers", e);
                }
            }
        }
        return renderType;
    }

    @Nonnull
    private static Object makeVanillaType(GpuTextureView texture) {
        Supplier<Object> sampler = SamplerCompat.isSupported() ? DEFAULT_SAMPLER : null;
        /*return new TextRenderType("modern_text_vanilla", 256, () -> {
            VANILLA_STATES.forEach(RenderStateShard::setupRenderState);
            //RenderSystem.setShaderTexture(0, texture);
        }, () -> VANILLA_STATES.forEach(RenderStateShard::clearRenderState));*/
        return MuiModApi.get().createRenderType("modern_text_vanilla", 256,
                false, true, RenderPipelines.TEXT,
                getOrRegisterTextureId(texture),
                sampler,
                true);
    }

    @Nonnull
    private static Object makeSeeThroughType(GpuTextureView texture) {
        Supplier<Object> sampler = SamplerCompat.isSupported() ? DEFAULT_SAMPLER : null;
        /*return new TextRenderType("modern_text_see_through", 256, () -> {
            SEE_THROUGH_STATES.forEach(RenderStateShard::setupRenderState);
            //RenderSystem.setShaderTexture(0, texture);
        }, () -> SEE_THROUGH_STATES.forEach(RenderStateShard::clearRenderState));*/
        return MuiModApi.get().createRenderType("modern_text_see_through", 256,
                false, true, RenderPipelines.TEXT_SEE_THROUGH,
                getOrRegisterTextureId(texture),
                sampler,
                true);
    }

    @Nonnull
    private static Object makePolygonOffsetType(GpuTextureView texture) {
        Supplier<Object> sampler = SamplerCompat.isSupported() ? DEFAULT_SAMPLER : null;
        /*return new TextRenderType("modern_text_polygon_offset", 256, () -> {
            POLYGON_OFFSET_STATES.forEach(RenderStateShard::setupRenderState);
            //RenderSystem.setShaderTexture(0, texture);
        }, () -> POLYGON_OFFSET_STATES.forEach(RenderStateShard::clearRenderState));*/
        return MuiModApi.get().createRenderType("modern_text_polygon_offset", 256,
                false, true, RenderPipelines.TEXT_POLYGON_OFFSET,
                getOrRegisterTextureId(texture),
                sampler,
                true);
    }

    /**
     * Batch rendering and custom ordering.
     * <p>
     * We use a single atlas for batch rendering to improve performance.
     */
    @Nullable
    public static Object getFirstSDFFillType() {
        return sFirstSDFFillType;
    }

    /**
     * Similarly, but for outline.
     *
     * @see #getFirstSDFFillType()
     */
    @Nullable
    public static Object getFirstSDFStrokeType() {
        return sFirstSDFStrokeType;
    }

    public static synchronized void clear(boolean cleanup) {
        if (sFirstSDFFillType != null) {
            assert (!sSDFFillTypes.isEmpty());
            var access = (AccessBufferSource) Minecraft.getInstance().renderBuffers().bufferSource();
            try {
                access.getFixedBuffers().remove(sFirstSDFFillType, sFirstSDFFillBuffer);
            } catch (Exception ignored) {
            }
            sFirstSDFFillType = null;
        }
        if (sFirstSDFStrokeType != null) {
            assert (!sSDFStrokeTypes.isEmpty());
            var access = (AccessBufferSource) Minecraft.getInstance().renderBuffers().bufferSource();
            try {
                access.getFixedBuffers().remove(sFirstSDFStrokeType, sFirstSDFStrokeBuffer);
            } catch (Exception ignored) {
            }
            sFirstSDFStrokeType = null;
        }
        sNormalTypes.clear();
        sSDFFillTypes.clear();
        sSDFStrokeTypes.clear();
        sVanillaTypes.clear();
        sSeeThroughTypes.clear();
        sPolygonOffsetTypes.clear();
        if (!sTextureIds.isEmpty()) {
            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            for (Object id : sTextureIds.values()) {
                TextureManagerCompat.release(textureManager, id);
            }
            sTextureIds.clear();
        }
        sFirstSDFFillBuffer.clear();
        sFirstSDFStrokeBuffer.clear();
        if (cleanup) {
            //sLinearFontSampler = RefCnt.move(sLinearFontSampler);
            /*sCurrentShaderSDFFill = null;
            sCurrentShaderSDFStroke = null;*/
        }
    }

    public static RenderPipeline getPipelineSDFFill() {
        return sCurrentPipelineSDFFill;
    }

    /*public static ShaderProgram getShaderNormal() {
        if (TextLayoutEngine.sCurrentInWorldRendering && !TextLayoutEngine.sUseTextShadersInWorld) {
            return CoreShaders.RENDERTYPE_TEXT;
        }
        return SHADER_NORMAL;
    }

    public static ShaderProgram getShaderSDFFill() {
        if (TextLayoutEngine.sCurrentInWorldRendering && !TextLayoutEngine.sUseTextShadersInWorld) {
            return CoreShaders.RENDERTYPE_TEXT;
        }
        return sCurrentShaderSDFFill;
    }

    public static ShaderProgram getShaderSDFStroke() {
        return sCurrentShaderSDFStroke;
    }*/

    public static synchronized boolean toggleSDFShaders(boolean smart) {
        if (smart) {
            try {
                // Caps is thread-safe, there's safe publication
                if (((GLCaps) Core.peekImmediateContext()
                        .getCaps()).getGLSLVersion() >= 400) {
                    if (sCurrentPipelineSDFFill != PIPELINE_SDF_FILL_SMART) {
                        sCurrentPipelineSDFFill = PIPELINE_SDF_FILL_SMART;
                        sCurrentPipelineSDFStroke = PIPELINE_SDF_STROKE_SMART;
                        return true;
                    }
                    return false;
                }
                LOGGER.info(MARKER, "No GLSL 400, smart SDF text shaders disabled");
            } catch (Throwable e) {
                LOGGER.warn(MARKER, "No GLSL 400, smart SDF text shaders disabled", e);
            }
        }
        if (sCurrentPipelineSDFFill != PIPELINE_SDF_FILL) {
            sCurrentPipelineSDFFill = PIPELINE_SDF_FILL;
            sCurrentPipelineSDFStroke = PIPELINE_SDF_STROKE;
            return true;
        }
        return false;
    }

    /*
     * Preload Modern UI text shaders for early text rendering.
     */
    /*public static synchronized void preloadShaders() {
        var provider = obtainResourceProvider();
        try {
            Minecraft.getInstance().getShaderManager().preloadForStartup(
                    provider, SHADER_NORMAL, SHADER_SDF_FILL, SHADER_SDF_STROKE
            );
        } catch (IOException | ShaderManager.CompilationException e) {
            throw new IllegalStateException("Bad text shaders", e);
        }
        toggleSDFShaders(false);
        LOGGER.info(MARKER, "Preloaded modern text shaders");
    }

    @Nonnull
    private static ResourceProvider obtainResourceProvider() {
        final var source = Minecraft.getInstance().getVanillaPackResources();
        final var fallback = source.asProvider();
        return location -> {
            // don't worry, ShaderManager will close it
            @SuppressWarnings("resource") final var stream = TextRenderType.class
                    .getResourceAsStream("/assets/" + location.getNamespace() + "/" + location.getPath());
            if (stream == null) {
                // fallback to vanilla
                return fallback.getResource(location);
            }
            return Optional.of(new Resource(source, () -> stream));
        };
    }*/
}

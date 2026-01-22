/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.mc.forge;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import icyllis.modernui.annotation.MainThread;
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.core.Core;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.mc.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Public APIs for Minecraft Forge mods to Modern UI.
 *
 * @since 3.3
 */
@OnlyIn(Dist.CLIENT)
public final class MuiForgeApi extends MuiModApi {

    @Nullable
    private static final MethodHandle RARITY_GET_STYLE_MODIFIER = findRarityGetStyleModifier();

    @Nullable
    private static MethodHandle findRarityGetStyleModifier() {
        try {
            return MethodHandles.publicLookup().findVirtual(
                    Rarity.class,
                    "getStyleModifier",
                    MethodType.methodType(UnaryOperator.class)
            );
        } catch (NoSuchMethodException | IllegalAccessException ignored) {
            return null;
        }
    }

    public MuiForgeApi() {
        ModernUIMod.LOGGER.info(ModernUIMod.MARKER, "Created MuiForgeAPI");
    }

    /**
     * Start the lifecycle of user interface with the fragment and create views.
     * This method must be called from client side main thread.
     * <p>
     * This is served as a local interaction model, the server will not intersect with this before.
     * Otherwise, initiate this with a network model via
     * {@link ServerPlayer#openMenu(MenuProvider, Consumer)}.
     * <p>
     * Specially, the main {@link Fragment} subclass can implement {@link ICapabilityProvider}
     * to provide capabilities, some of which may be internally handled by the framework.
     * For example, {@link ScreenCallback} to describe the screen properties.
     *
     * @param fragment the main fragment
     */
    @MainThread
    public static void openScreen(@Nonnull Fragment fragment) {
        MuiModApi.openScreen(fragment);
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    public <T extends Screen & MuiScreen> T createScreen(@Nonnull Fragment fragment,
                                                         @Nullable ScreenCallback callback,
                                                         @Nullable Screen previousScreen,
                                                         @Nullable CharSequence title) {
        return (T) new SimpleScreen(UIManager.getInstance(),
                fragment, callback, previousScreen, title);
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    public <T extends AbstractContainerMenu, U extends Screen & MenuAccess<T> & MuiScreen>
    U createMenuScreen(@Nonnull Fragment fragment,
                       @Nullable ScreenCallback callback,
                       @Nonnull T menu,
                       @Nonnull Inventory inventory,
                       @Nonnull Component title) {
        return (U) new MenuScreen<>(UIManager.getInstance(),
                fragment, callback, menu, inventory, title);
    }

    /**
     * Get the elapsed time since the current screen is set, updated every frame on Render thread.
     * Ignoring game paused.
     *
     * @return elapsed time in milliseconds
     */
    @RenderThread
    public static long getElapsedTime() {
        return MuiModApi.getElapsedTime();
    }

    /**
     * Get synced UI frame time, updated every frame on Render thread. Ignoring game paused.
     *
     * @return frame time in milliseconds
     */
    @RenderThread
    public static long getFrameTime() {
        return getFrameTimeNanos() / 1000000;
    }

    /**
     * Get synced UI frame time, updated every frame on Render thread. Ignoring game paused.
     *
     * @return frame time in nanoseconds
     */
    @RenderThread
    public static long getFrameTimeNanos() {
        return MuiModApi.getFrameTimeNanos();
    }

    /**
     * Post a runnable to be executed asynchronously (no barrier) on UI thread.
     * This method is equivalent to calling {@link Core#getUiHandlerAsync()},
     * but {@link Core} is not a public API.
     *
     * @param r the Runnable that will be executed
     */
    public static void postToUiThread(@Nonnull Runnable r) {
        MuiModApi.postToUiThread(r);
    }

    @Override
    public boolean isGLVersionPromoted() {
        try {
            String version = net.minecraftforge.fml.loading.ImmediateWindowHandler.getGLVersion();
            if (!"3.2".equals(version)) {
                ModernUIMod.LOGGER.info(ModernUIMod.MARKER, "Detected OpenGL {} Core Profile from FML Early Window",
                        version);
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    @Override
    public void loadEffect(GameRenderer gr, Object effect) {
        GameRendererCompat.setPostEffect(gr, effect);
    }

    /*@Override
    public ShaderInstance makeShaderInstance(ResourceProvider resourceProvider,
                                             ResourceLocation resourceLocation,
                                             VertexFormat vertexFormat) throws IOException {
        return new ShaderInstance(resourceProvider, resourceLocation, vertexFormat);
    }*/

    @Override
    public boolean isKeyBindingMatches(KeyMapping keyMapping, InputConstants.Key key) {
        int keyCode = key.getType() == InputConstants.Type.KEYSYM
                ? key.getValue()
                : InputConstants.UNKNOWN.getValue();
        int scanCode = key.getType() == InputConstants.Type.SCANCODE
                ? key.getValue()
                : InputConstants.UNKNOWN.getValue();
        return keyMapping.matches(new KeyEvent(keyCode, scanCode, /*modifiers*/ 0));
    }

    @Override
    public Style applyRarityTo(Rarity rarity, Style baseStyle) {
        MethodHandle getStyleModifier = RARITY_GET_STYLE_MODIFIER;
        if (getStyleModifier != null) {
            try {
                @SuppressWarnings("unchecked")
                UnaryOperator<Style> modifier = (UnaryOperator<Style>) getStyleModifier.invoke(rarity);
                if (modifier != null) {
                    return modifier.apply(baseStyle);
                }
            } catch (Throwable ignored) {
            }
        }
        return baseStyle.withColor(rarity.color());
    }

    @Override
    public GpuDevice getRealGpuDevice() {
        GpuDevice gpuDevice = RenderSystem.getDevice();
        return gpuDevice;
    }

    @Override
    public GpuTexture getRealGpuTexture(GpuTexture faker) {
        GpuTexture gpuTexture = faker;
        return gpuTexture;
    }

    @Override
    public void submitGuiElementRenderState(GuiGraphics graphics, GuiElementRenderState renderState) {
        graphics.getRenderState().submitGuiElement(renderState);
    }

    @Override
    public void submitPictureInPictureRenderState(GuiGraphics graphics, PictureInPictureRenderState renderState) {
        graphics.getRenderState().submitPicturesInPictureState(renderState);
    }

    @Nullable
    @Override
    public ScreenRectangle peekScissorStack(GuiGraphics graphics) {
        return graphics.getScissorStack().peek();
    }

    @Override
    public <RT> RT createRenderType(String name, int bufferSize,
                                   boolean affectsCrumbling, boolean sortOnUpload,
                                   RenderPipeline renderPipeline,
                                   @Nullable Object texture,
                                   @Nullable Supplier<?> sampler,
                                   boolean lightmap) {
        return RenderTypeCompat.create(
                name,
                bufferSize,
                affectsCrumbling,
                sortOnUpload,
                renderPipeline,
                texture,
                sampler,
                lightmap
        );
    }

    /* Screen */
    /*public static int getScreenBackgroundColor() {
        return (int) (BlurHandler.INSTANCE.getBackgroundAlpha() * 255.0f) << 24;
    }*/

    /* Minecraft */
    /*public static void displayInGameMenu(boolean usePauseScreen) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.currentScreen == null) {
            // If press F3 + Esc and is single player and not open LAN world
            if (usePauseScreen && minecraft.isIntegratedServerRunning() && minecraft.getIntegratedServer() != null &&
             !minecraft.getIntegratedServer().getPublic()) {
                minecraft.displayGuiScreen(new IngameMenuScreen(false));
                minecraft.getSoundHandler().pause();
            } else {
                //UIManager.INSTANCE.openGuiScreen(new TranslationTextComponent("menu.game"), IngameMenuHome::new);
                minecraft.displayGuiScreen(new IngameMenuScreen(true));
            }
        }
    }*/
}

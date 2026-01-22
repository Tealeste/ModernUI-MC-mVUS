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

package icyllis.modernui.mc;

import net.minecraft.client.*;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

@ApiStatus.Internal
public final class OptiFineIntegration {

    private static Field of_fast_render;
    private static Field shaderPackLoaded;
    private static final AtomicBoolean WARNED_OPEN_SHADERS_GUI = new AtomicBoolean();
    private static final AtomicBoolean WARNED_SET_FAST_RENDER = new AtomicBoolean();
    private static final AtomicBoolean WARNED_SET_GUI_SCALE = new AtomicBoolean();
    private static final AtomicBoolean WARNED_SHADERPACK_LOADED = new AtomicBoolean();
    private static final AtomicBoolean WARNED_INIT_REFLECTION = new AtomicBoolean();

    static {
        try {
            of_fast_render = Options.class.getDeclaredField("ofFastRender");
        } catch (NoSuchFieldException ignored) {
            // OptiFine not installed (or option removed); integration becomes a no-op.
        } catch (Throwable t) {
            if (WARNED_INIT_REFLECTION.compareAndSet(false, true)) {
                ModernUIMod.LOGGER.warn(ModernUIMod.MARKER,
                        "Failed to initialize OptiFine integration (ofFastRender); feature will be unavailable.", t);
            }
        }
        try {
            Class<?> clazz = Class.forName("net.optifine.shaders.Shaders");
            shaderPackLoaded = clazz.getDeclaredField("shaderPackLoaded");
        } catch (ClassNotFoundException | NoSuchFieldException ignored) {
            // OptiFine shaders not installed; integration becomes a no-op.
        } catch (Throwable t) {
            if (WARNED_INIT_REFLECTION.compareAndSet(false, true)) {
                ModernUIMod.LOGGER.warn(ModernUIMod.MARKER,
                        "Failed to initialize OptiFine integration (Shaders.shaderPackLoaded); assuming no shader pack.",
                        t);
            }
        }
    }

    private OptiFineIntegration() {
    }

    public static void openShadersGui() {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            Class<?> clazz = Class.forName("net.optifine.shaders.gui.GuiShaders");
            Constructor<?> constructor = clazz.getConstructor(Screen.class, Options.class);
            minecraft.setScreen((Screen) constructor.newInstance(minecraft.screen, minecraft.options));
        } catch (Throwable t) {
            if (WARNED_OPEN_SHADERS_GUI.compareAndSet(false, true)) {
                ModernUIMod.LOGGER.warn(ModernUIMod.MARKER,
                        "Failed to open OptiFine shaders screen.", t);
            }
        }
    }

    /**
     * Incompatible with TextMC. Because we break the Vanilla rendering order.
     * See TextRenderNode#drawText()  endBatch(Sheets.signSheet()).
     * Modern UI glyph texture is translucent, so ending sign rendering earlier
     * stops sign texture being discarded by depth test.
     */
    public static void setFastRender(boolean fastRender) {
        Minecraft minecraft = Minecraft.getInstance();
        if (of_fast_render != null) {
            try {
                of_fast_render.setBoolean(minecraft.options, fastRender);
                minecraft.options.save();
            } catch (Throwable t) {
                if (WARNED_SET_FAST_RENDER.compareAndSet(false, true)) {
                    ModernUIMod.LOGGER.warn(ModernUIMod.MARKER,
                            "Failed to update OptiFine fast render option.", t);
                }
            }
        }
    }

    public static void setGuiScale(OptionInstance<Integer> option) {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            Field field = Options.class.getDeclaredField("GUI_SCALE");
            field.setAccessible(true);
            field.set(minecraft.options, option);
        } catch (Throwable t) {
            if (WARNED_SET_GUI_SCALE.compareAndSet(false, true)) {
                ModernUIMod.LOGGER.warn(ModernUIMod.MARKER,
                        "Failed to apply OptiFine GUI scale option replacement.", t);
            }
        }
    }

    public static boolean isShaderPackLoaded() {
        if (shaderPackLoaded != null) {
            try {
                return shaderPackLoaded.getBoolean(null);
            } catch (Throwable t) {
                if (WARNED_SHADERPACK_LOADED.compareAndSet(false, true)) {
                    ModernUIMod.LOGGER.warn(ModernUIMod.MARKER,
                            "Failed to query OptiFine shader pack state; assuming no shader pack is active.", t);
                }
            }
        }
        return false;
    }
}

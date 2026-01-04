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

package icyllis.modernui.mc;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.input.KeyEvent;

import static org.lwjgl.glfw.GLFW.*;

public final class KeyCompat {

    private KeyCompat() {
    }

    public static long windowHandle(Window window) {
        // Prefer direct call: remapped by Loom for production Fabric (no string-based reflection).
        return window.handle();
    }

    public static boolean isKeyDown(Window window, int key) {
        // Avoid name-based reflection against InputConstants (intermediary obfuscates method names in production Fabric).
        return glfwGetKey(windowHandle(window), key) == GLFW_PRESS;
    }

    public static InputConstants.Key getKey(int keyCode, int scanCode) {
        return getKey(keyCode, scanCode, /*modifiers*/ 0);
    }

    public static InputConstants.Key getKey(int keyCode, int scanCode, int modifiers) {
        // Avoid name-based reflection (production Fabric uses intermediary) and use the KeyEvent-based API.
        return InputConstants.getKey(new KeyEvent(keyCode, scanCode, modifiers));
    }

    public static boolean isControlDown(Window window) {
        return isKeyDown(window, GLFW_KEY_LEFT_CONTROL) || isKeyDown(window, GLFW_KEY_RIGHT_CONTROL);
    }

    public static boolean isShiftDown(Window window) {
        return isKeyDown(window, GLFW_KEY_LEFT_SHIFT) || isKeyDown(window, GLFW_KEY_RIGHT_SHIFT);
    }

    public static boolean isAltDown(Window window) {
        return isKeyDown(window, GLFW_KEY_LEFT_ALT) || isKeyDown(window, GLFW_KEY_RIGHT_ALT);
    }

}

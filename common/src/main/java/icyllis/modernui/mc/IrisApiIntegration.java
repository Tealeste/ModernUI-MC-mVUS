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

import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

@ApiStatus.Internal
public final class IrisApiIntegration {

    private static Object irisApiInstance;
    private static Method isShaderPackInUse;
    private static final AtomicBoolean WARNED_INVOKE_FAILURE = new AtomicBoolean();

    static {
        try {
            Class<?> clazz = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            irisApiInstance = clazz.getMethod("getInstance").invoke(null);
            isShaderPackInUse = clazz.getMethod("isShaderPackInUse");
        } catch (ClassNotFoundException ignored) {
            // Iris not installed.
        } catch (Throwable t) {
            ModernUIMod.LOGGER.warn(ModernUIMod.MARKER,
                    "Failed to initialize Iris API integration; assuming no shader pack is active.", t);
        }
    }

    private IrisApiIntegration() {
    }

    public static boolean isShaderPackInUse() {
        if (isShaderPackInUse != null) {
            try {
                return (boolean) isShaderPackInUse.invoke(irisApiInstance);
            } catch (Throwable t) {
                if (WARNED_INVOKE_FAILURE.compareAndSet(false, true)) {
                    ModernUIMod.LOGGER.warn(ModernUIMod.MARKER,
                            "Failed to query Iris shader pack state; assuming no shader pack is active.", t);
                }
            }
        }
        return false;
    }
}

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

package icyllis.modernui.mc.compat;

import org.jetbrains.annotations.ApiStatus;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@ApiStatus.Internal
public final class ModListCompat {

    private static final Object INSTANCE;
    private static final MethodHandle IS_LOADED;

    static {
        Object instance = null;
        MethodHandle isLoaded = null;

        // Forge: net.minecraftforge.fml.ModList
        try {
            Class<?> modListClass = Class.forName("net.minecraftforge.fml.ModList");
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            MethodHandle get = lookup.findStatic(modListClass, "get", MethodType.methodType(modListClass));
            instance = get.invoke();
            isLoaded = lookup.findVirtual(modListClass, "isLoaded", MethodType.methodType(boolean.class, String.class));
        } catch (Throwable ignored) {
        }

        // NeoForge: net.neoforged.fml.ModList
        if (isLoaded == null) {
            try {
                Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                MethodHandle get = lookup.findStatic(modListClass, "get", MethodType.methodType(modListClass));
                instance = get.invoke();
                isLoaded = lookup.findVirtual(modListClass, "isLoaded", MethodType.methodType(boolean.class, String.class));
            } catch (Throwable ignored) {
            }
        }

        INSTANCE = instance;
        IS_LOADED = isLoaded;
    }

    private ModListCompat() {
    }

    public static boolean isLoaded(String modId) {
        if (INSTANCE == null || IS_LOADED == null) {
            return false;
        }
        try {
            return (boolean) IS_LOADED.invoke(INSTANCE, modId);
        } catch (Throwable ignored) {
            return false;
        }
    }
}

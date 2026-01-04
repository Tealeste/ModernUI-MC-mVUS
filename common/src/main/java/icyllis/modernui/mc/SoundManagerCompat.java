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

package icyllis.modernui.mc;

import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.sounds.SoundSource;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Patch-level compatibility helpers for {@link SoundManager} volume update APIs.
 */
public final class SoundManagerCompat {

    private static final Method UPDATE_VOLUME = resolveVolumeUpdate();
    private static final Method GET_SOUND_EVENT = resolveGetSoundEvent();

    private SoundManagerCompat() {
    }

    public static void updateCategoryVolume(SoundManager soundManager, SoundSource source, float volume) {
        try {
            UPDATE_VOLUME.invoke(soundManager, source, volume);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update SoundManager volume", e);
        }
    }

    public static Object getSoundEvent(SoundManager soundManager, Object soundEventId) {
        try {
            return GET_SOUND_EVENT.invoke(soundManager, soundEventId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to query SoundManager sound event", e);
        }
    }

    private static Method resolveVolumeUpdate() {
        for (Method method : SoundManager.class.getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getReturnType() != void.class || method.getParameterCount() != 2) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params[0] != SoundSource.class || params[1] != float.class) {
                continue;
            }
            method.setAccessible(true);
            return method;
        }
        for (Method method : SoundManager.class.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getReturnType() != void.class || method.getParameterCount() != 2) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params[0] != SoundSource.class || params[1] != float.class) {
                continue;
            }
            method.setAccessible(true);
            return method;
        }
        throw new IllegalStateException("No compatible SoundManager volume update method found");
    }

    private static Method resolveGetSoundEvent() {
        Class<?> idClass = ResourceIdCompat.idClass();
        for (Method method : SoundManager.class.getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getParameterCount() != 1 || method.getReturnType() != WeighedSoundEvents.class) {
                continue;
            }
            if (method.getParameterTypes()[0] != idClass) {
                continue;
            }
            method.setAccessible(true);
            return method;
        }
        for (Method method : SoundManager.class.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getParameterCount() != 1 || method.getReturnType() != WeighedSoundEvents.class) {
                continue;
            }
            if (method.getParameterTypes()[0] != idClass) {
                continue;
            }
            method.setAccessible(true);
            return method;
        }
        throw new IllegalStateException("No compatible SoundManager#getSoundEvent(id) method found");
    }
}

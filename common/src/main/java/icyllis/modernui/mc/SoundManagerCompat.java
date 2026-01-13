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

import net.minecraft.client.Options;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Patch-level compatibility helpers for {@link SoundManager} volume update APIs.
 */
public final class SoundManagerCompat {

    private static final Marker MARKER = MarkerManager.getMarker("Sound");

    private static final Object UPDATE_VOLUME_LOCK = new Object();
    private static final AtomicBoolean UPDATE_VOLUME_WARNED = new AtomicBoolean(false);
    private static volatile boolean UPDATE_VOLUME_RESOLVED;
    private static volatile Method UPDATE_CATEGORY_VOLUME;
    private static volatile Field SOUND_ENGINE_FIELD;
    private static volatile Field SOUND_ENGINE_OPTIONS_FIELD;
    private static volatile Field SOUND_ENGINE_INSTANCE_TO_CHANNEL_FIELD;
    private static volatile Method SOUND_ENGINE_SET_VOLUME;

    private static final Object GET_SOUND_EVENT_LOCK = new Object();
    private static final AtomicBoolean GET_SOUND_EVENT_WARNED = new AtomicBoolean(false);
    private static volatile boolean GET_SOUND_EVENT_RESOLVED;
    private static volatile Method GET_SOUND_EVENT;

    private SoundManagerCompat() {
    }

    public static void updateCategoryVolume(SoundManager soundManager, SoundSource source, float volume) {
        ensureUpdateVolumeResolved();
        if (UPDATE_CATEGORY_VOLUME != null) {
            try {
                UPDATE_CATEGORY_VOLUME.invoke(soundManager, source, volume);
            } catch (Throwable t) {
                warnOnce(UPDATE_VOLUME_WARNED,
                        "Failed to invoke SoundManager volume update method; disabling volume multiplier compat",
                        t);
            }
            return;
        }
        if (SOUND_ENGINE_SET_VOLUME != null &&
                SOUND_ENGINE_FIELD != null &&
                SOUND_ENGINE_OPTIONS_FIELD != null &&
                SOUND_ENGINE_INSTANCE_TO_CHANNEL_FIELD != null) {
            try {
                updateVolumeViaSoundEngine(soundManager, source, volume);
            } catch (Throwable t) {
                warnOnce(UPDATE_VOLUME_WARNED,
                        "Failed to apply SoundEngine volume multiplier compat; disabling volume multiplier compat",
                        t);
            }
            return;
        }
        warnOnce(UPDATE_VOLUME_WARNED,
                "No compatible SoundManager/SoundEngine volume update method found; volume multiplier will be ignored",
                null);
    }

    private static void updateVolumeViaSoundEngine(SoundManager soundManager, SoundSource source, float volume) throws Exception {
        Object engineObj = SOUND_ENGINE_FIELD.get(soundManager);
        if (!(engineObj instanceof SoundEngine soundEngine)) {
            return;
        }
        Object optionsObj = SOUND_ENGINE_OPTIONS_FIELD.get(soundEngine);
        if (!(optionsObj instanceof Options options)) {
            return;
        }
        float currentFinalVolume = options.getFinalSoundSourceVolume(source);
        float multiplier;
        if (currentFinalVolume == 0.0f) {
            multiplier = volume == 0.0f ? 0.0f : 1.0f;
        } else {
            multiplier = volume / currentFinalVolume;
        }
        Object mapObj = SOUND_ENGINE_INSTANCE_TO_CHANNEL_FIELD.get(soundEngine);
        if (!(mapObj instanceof Map<?, ?> instanceToChannel)) {
            return;
        }
        for (Object instanceObj : instanceToChannel.keySet()) {
            if (!(instanceObj instanceof SoundInstance soundInstance)) {
                continue;
            }
            // MASTER affects all sources; other sources only affect their own instances.
            if (source != SoundSource.MASTER && soundInstance.getSource() != source) {
                continue;
            }
            SOUND_ENGINE_SET_VOLUME.invoke(soundEngine, soundInstance, multiplier);
        }
    }

    private static void ensureUpdateVolumeResolved() {
        if (UPDATE_VOLUME_RESOLVED) {
            return;
        }
        synchronized (UPDATE_VOLUME_LOCK) {
            if (UPDATE_VOLUME_RESOLVED) {
                return;
            }
            UPDATE_CATEGORY_VOLUME = resolveUpdateCategoryVolume();
            if (UPDATE_CATEGORY_VOLUME == null) {
                SOUND_ENGINE_FIELD = resolveSoundEngineField();
                SOUND_ENGINE_OPTIONS_FIELD = resolveSoundEngineOptionsField();
                SOUND_ENGINE_INSTANCE_TO_CHANNEL_FIELD = resolveSoundEngineInstanceToChannelField();
                SOUND_ENGINE_SET_VOLUME = resolveSoundEngineSetVolume();
            }
            UPDATE_VOLUME_RESOLVED = true;
        }
    }

    private static Method resolveUpdateCategoryVolume() {
        // 1.21.11+ (and possibly other versions) expose a direct (SoundSource,float) API on SoundManager.
        for (Method method : SoundManager.class.getMethods()) {
            Method candidate = tryMatchUpdateCategoryVolume(method);
            if (candidate != null) {
                return candidate;
            }
        }
        for (Method method : SoundManager.class.getDeclaredMethods()) {
            Method candidate = tryMatchUpdateCategoryVolume(method);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private static Method tryMatchUpdateCategoryVolume(Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            return null;
        }
        if (method.getReturnType() != void.class || method.getParameterCount() != 2) {
            return null;
        }
        Class<?>[] params = method.getParameterTypes();
        if (params[0] != SoundSource.class || params[1] != float.class) {
            return null;
        }
        method.setAccessible(true);
        return method;
    }

    private static Field resolveSoundEngineField() {
        for (Field field : SoundManager.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (field.getType() != SoundEngine.class) {
                continue;
            }
            field.setAccessible(true);
            return field;
        }
        return null;
    }

    private static Field resolveSoundEngineOptionsField() {
        for (Field field : SoundEngine.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (field.getType() != Options.class) {
                continue;
            }
            field.setAccessible(true);
            return field;
        }
        return null;
    }

    private static Field resolveSoundEngineInstanceToChannelField() {
        Field candidate = null;
        for (Field field : SoundEngine.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (!Map.class.isAssignableFrom(field.getType())) {
                continue;
            }
            Type genericType = field.getGenericType();
            if (!(genericType instanceof ParameterizedType parameterizedType)) {
                continue;
            }
            Type[] args = parameterizedType.getActualTypeArguments();
            if (args.length != 2 || args[0] != SoundInstance.class) {
                continue;
            }
            // SoundEngine contains multiple Map<SoundInstance, Integer> maps; the one we want is Map<SoundInstance, ChannelHandle>.
            if (args[1] == Integer.class) {
                continue;
            }
            if (candidate != null) {
                return null;
            }
            candidate = field;
        }
        if (candidate != null) {
            candidate.setAccessible(true);
        }
        return candidate;
    }

    private static Method resolveSoundEngineSetVolume() {
        // 1.21.10 and older provide a (SoundInstance, float) API on SoundEngine that multiplies volume.
        for (Method method : SoundEngine.class.getMethods()) {
            Method candidate = tryMatchSoundEngineSetVolume(method);
            if (candidate != null) {
                return candidate;
            }
        }
        for (Method method : SoundEngine.class.getDeclaredMethods()) {
            Method candidate = tryMatchSoundEngineSetVolume(method);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private static Method tryMatchSoundEngineSetVolume(Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            return null;
        }
        if (method.getReturnType() != void.class || method.getParameterCount() != 2) {
            return null;
        }
        Class<?>[] params = method.getParameterTypes();
        if (params[0] != SoundInstance.class || params[1] != float.class) {
            return null;
        }
        method.setAccessible(true);
        return method;
    }

    private static void warnOnce(AtomicBoolean warned, String message, Throwable t) {
        if (!warned.compareAndSet(false, true)) {
            return;
        }
        if (t != null) {
            ModernUIMod.LOGGER.warn(MARKER, message, t);
        } else {
            ModernUIMod.LOGGER.warn(MARKER, message);
        }
    }

    public static Object getSoundEvent(SoundManager soundManager, Object soundEventId) {
        Method method = getSoundEventMethod();
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(soundManager, soundEventId);
        } catch (Throwable t) {
            warnOnce(GET_SOUND_EVENT_WARNED,
                    "Failed to invoke SoundManager#getSoundEvent(id) compat method; treating as unavailable",
                    t);
            return null;
        }
    }

    private static Method getSoundEventMethod() {
        if (GET_SOUND_EVENT_RESOLVED) {
            return GET_SOUND_EVENT;
        }
        synchronized (GET_SOUND_EVENT_LOCK) {
            if (GET_SOUND_EVENT_RESOLVED) {
                return GET_SOUND_EVENT;
            }
            GET_SOUND_EVENT = resolveGetSoundEvent();
            GET_SOUND_EVENT_RESOLVED = true;
            if (GET_SOUND_EVENT == null) {
                warnOnce(GET_SOUND_EVENT_WARNED,
                        "No compatible SoundManager#getSoundEvent(id) method found; sound id checks will be skipped",
                        null);
            }
            return GET_SOUND_EVENT;
        }
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
        return null;
    }
}

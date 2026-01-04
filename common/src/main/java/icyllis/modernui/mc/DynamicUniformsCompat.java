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

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.DynamicUniforms;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.joml.Vector4fc;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Patch-level compatibility helpers for {@link DynamicUniforms#writeTransform} signature drift.
 */
public final class DynamicUniformsCompat {

    private static final Method WRITE_TRANSFORM_5 = resolveMethod(
            Matrix4fc.class, Vector4fc.class, Vector3fc.class, Matrix4fc.class, float.class
    );
    private static final Method WRITE_TRANSFORM_4 = resolveMethod(
            Matrix4fc.class, Vector4fc.class, Vector3fc.class, Matrix4fc.class
    );

    private DynamicUniformsCompat() {
    }

    public static GpuBufferSlice writeTransform(DynamicUniforms uniforms,
                                               Matrix4fc localMat,
                                               Vector4fc pushData0,
                                               Vector3fc pushData1,
                                               Matrix4fc pushData2to5,
                                               float extraFloat) {
        try {
            if (WRITE_TRANSFORM_5 != null) {
                return (GpuBufferSlice) WRITE_TRANSFORM_5.invoke(
                        uniforms, localMat, pushData0, pushData1, pushData2to5, extraFloat
                );
            }
            if (WRITE_TRANSFORM_4 != null) {
                return (GpuBufferSlice) WRITE_TRANSFORM_4.invoke(
                        uniforms, localMat, pushData0, pushData1, pushData2to5
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke DynamicUniforms#writeTransform", e);
        }
        throw new IllegalStateException("No compatible DynamicUniforms#writeTransform overload found");
    }

    private static Method resolveMethod(Class<?>... parameterTypes) {
        for (Method method : DynamicUniforms.class.getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (!GpuBufferSlice.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            if (!java.util.Arrays.equals(method.getParameterTypes(), parameterTypes)) {
                continue;
            }
            return method;
        }
        for (Method method : DynamicUniforms.class.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (!GpuBufferSlice.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            if (!java.util.Arrays.equals(method.getParameterTypes(), parameterTypes)) {
                continue;
            }
            method.setAccessible(true);
            return method;
        }
        return null;
    }
}

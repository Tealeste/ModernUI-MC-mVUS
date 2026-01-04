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

import com.mojang.blaze3d.pipeline.RenderPipeline;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;

public final class RenderPipelineCompat {

    private static final Method WITH_LOCATION = findNonStringOverload("withLocation");
    private static final Method WITH_VERTEX_SHADER = findNonStringOverload("withVertexShader");
    private static final Method WITH_FRAGMENT_SHADER = findNonStringOverload("withFragmentShader");

    private RenderPipelineCompat() {
    }

    @Nonnull
    public static RenderPipeline.Builder withLocation(@Nonnull RenderPipeline.Builder builder, @Nonnull Object id) {
        return invoke(builder, WITH_LOCATION, id);
    }

    @Nonnull
    public static RenderPipeline.Builder withVertexShader(@Nonnull RenderPipeline.Builder builder, @Nonnull Object id) {
        return invoke(builder, WITH_VERTEX_SHADER, id);
    }

    @Nonnull
    public static RenderPipeline.Builder withFragmentShader(@Nonnull RenderPipeline.Builder builder, @Nonnull Object id) {
        return invoke(builder, WITH_FRAGMENT_SHADER, id);
    }

    private static Method findNonStringOverload(String name) {
        for (Method method : RenderPipeline.Builder.class.getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != 1) {
                continue;
            }
            if (method.getParameterTypes()[0] == String.class) {
                continue;
            }
            return method;
        }
        throw new IllegalStateException("No compatible RenderPipeline.Builder#" + name + "(...) overload found");
    }

    private static RenderPipeline.Builder invoke(RenderPipeline.Builder builder, Method method, Object arg) {
        try {
            return (RenderPipeline.Builder) method.invoke(builder, arg);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke RenderPipeline.Builder#" + method.getName() + "(...)", e);
        }
    }
}


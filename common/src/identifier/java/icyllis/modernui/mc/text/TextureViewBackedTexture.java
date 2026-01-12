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

package icyllis.modernui.mc.text;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.renderer.texture.AbstractTexture;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Supplier;

final class TextureViewBackedTexture extends AbstractTexture {

    private final Supplier<?> samplerSupplier;
    private volatile GpuSampler cachedSampler;

    TextureViewBackedTexture(@Nonnull GpuTextureView view, @Nonnull Supplier<?> samplerSupplier) {
        this.textureView = Objects.requireNonNull(view, "view");
        this.texture = view.texture();
        this.samplerSupplier = Objects.requireNonNull(samplerSupplier, "samplerSupplier");
        // AbstractTexture.sampler is protected, so we can set it directly without reflection.
        this.sampler = getSamplerValue();
    }

    @Override
    public void close() {
        // Lifetime is managed by the caller.
    }

    private GpuSampler getSamplerValue() {
        GpuSampler cached = cachedSampler;
        if (cached != null) {
            return cached;
        }
        Object value = samplerSupplier.get();
        if (!(value instanceof GpuSampler sampler)) {
            return null;
        }
        cachedSampler = sampler;
        return sampler;
    }
}

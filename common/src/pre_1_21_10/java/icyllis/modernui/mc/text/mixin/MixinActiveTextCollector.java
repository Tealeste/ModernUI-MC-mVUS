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

package icyllis.modernui.mc.text.mixin;

import net.minecraft.client.gui.render.state.GuiTextRenderState;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 1.21.8 and older: {@code net.minecraft.client.gui.ActiveTextCollector} does not exist yet, so the 1.21.11+
 * hit-testing fix is not applicable. Keep this class present so it can stay listed in the shared mixin config.
 */
@Mixin(GuiTextRenderState.class)
public class MixinActiveTextCollector {
}


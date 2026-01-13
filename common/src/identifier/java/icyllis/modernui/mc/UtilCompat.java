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

import net.minecraft.util.Util;

import java.util.concurrent.Executor;

/**
 * Compatibility helpers for {@link Util}.
 */
public final class UtilCompat {

    private UtilCompat() {
    }

    public static Executor ioPool() {
        return Util.ioPool();
    }

    public static long getMillis() {
        return Util.getMillis();
    }

    public static long getNanos() {
        return Util.getNanos();
    }

    public static int offsetByCodepoints(String text, int cursor, int direction) {
        return Util.offsetByCodepoints(text, cursor, direction);
    }
}

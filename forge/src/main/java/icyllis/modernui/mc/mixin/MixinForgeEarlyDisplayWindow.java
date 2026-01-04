/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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

package icyllis.modernui.mc.mixin;

import net.minecraftforge.fml.earlydisplay.DisplayWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@Mixin(value = DisplayWindow.class, remap = false)
public class MixinForgeEarlyDisplayWindow {

    @Shadow
    private Method loadingOverlay;

    /**
     * @author BloCamLimb
     * @reason ModernUI dev runtime compatibility
     */
    @Overwrite
    public void updateModuleReads(ModuleLayer layer) {
        String forgeModuleName = "net.minecraftforge.forge";
        Module forgeModule = layer.findModule(forgeModuleName).orElse(null);
        if (forgeModule == null) {
            throw new IllegalStateException("Could not find '" + forgeModuleName + "' in module layer: " + layer);
        }

        this.getClass().getModule().addReads(forgeModule);

        Class<?> overlayClass;
        try {
            overlayClass = Class.forName(forgeModule, "net.minecraftforge.client.loading.ForgeLoadingOverlay");
        } catch (Throwable t) {
            overlayClass = null;
        }
        if (overlayClass == null) {
            try {
                overlayClass = Class.forName("net.minecraftforge.client.loading.ForgeLoadingOverlay");
            } catch (ClassNotFoundException e) {
                return;
            }
        }

        for (Method method : overlayClass.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) && "newInstance".equals(method.getName())) {
                loadingOverlay = method;
                break;
            }
        }
        if (loadingOverlay == null) {
            throw new IllegalStateException(overlayClass.getName() + " does not have a static newInstance method");
        }
    }
}


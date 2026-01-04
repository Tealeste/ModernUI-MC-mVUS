package icyllis.modernui.mc;

import net.minecraft.client.Camera;

import java.lang.reflect.Method;

public final class CameraCompat {

    private static final Method X_ROT = resolveMethod("xRot");
    private static final Method GET_X_ROT = resolveMethod("getXRot");
    private static final Method Y_ROT = resolveMethod("yRot");
    private static final Method GET_Y_ROT = resolveMethod("getYRot");

    private CameraCompat() {
    }

    public static float xRot(Camera camera) {
        return invokeFloat(camera, X_ROT, GET_X_ROT);
    }

    public static float yRot(Camera camera) {
        return invokeFloat(camera, Y_ROT, GET_Y_ROT);
    }

    private static float invokeFloat(Camera camera, Method preferred, Method fallback) {
        try {
            if (preferred != null) {
                return ((Number) preferred.invoke(camera)).floatValue();
            }
            if (fallback != null) {
                return ((Number) fallback.invoke(camera)).floatValue();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read camera rotation", e);
        }
        throw new IllegalStateException("No compatible camera rotation accessor found");
    }

    private static Method resolveMethod(String name) {
        try {
            return Camera.class.getMethod(name);
        } catch (Exception ignored) {
            return null;
        }
    }
}

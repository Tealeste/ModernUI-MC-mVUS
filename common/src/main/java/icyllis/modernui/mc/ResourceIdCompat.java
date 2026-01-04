package icyllis.modernui.mc;

import java.lang.reflect.Constructor;

/**
 * Patch-level compatibility helpers for Minecraft's resource id type.
 * <p>
 * In Mojang-mapped dev/Forge environments this is typically {@code net.minecraft.resources.Identifier}
 * (1.21.11+) or {@code net.minecraft.resources.ResourceLocation} (<= 1.21.10).
 * In Fabric production (intermediary) this becomes {@code net.minecraft.class_2960}.
 */
public final class ResourceIdCompat {

    private static final Class<?> ID_CLASS = resolveIdClass();
    private static final Constructor<?> ID_CTOR = resolveIdConstructor();

    private static final String DEFAULT_NAMESPACE = "minecraft";

    private ResourceIdCompat() {
    }

    static Class<?> idClass() {
        return ID_CLASS;
    }

    @SuppressWarnings("unchecked")
    public static <T> T fromNamespaceAndPath(String namespace, String path) {
        try {
            return (T) ID_CTOR.newInstance(namespace, path);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create resource id from namespace/path", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T withDefaultNamespace(String path) {
        return fromNamespaceAndPath(DEFAULT_NAMESPACE, path);
    }

    @SuppressWarnings("unchecked")
    public static <T> T tryParse(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        String namespace = DEFAULT_NAMESPACE;
        String path = id;
        int separatorIndex = id.indexOf(':');
        if (separatorIndex >= 0) {
            if (separatorIndex == 0 || separatorIndex == id.length() - 1) {
                return null;
            }
            namespace = id.substring(0, separatorIndex);
            path = id.substring(separatorIndex + 1);
        }
        try {
            return (T) ID_CTOR.newInstance(namespace, path);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String namespace(Object id) {
        if (id == null) {
            throw new NullPointerException("id");
        }
        String value = id.toString();
        int separatorIndex = value.indexOf(':');
        if (separatorIndex < 0) {
            return DEFAULT_NAMESPACE;
        }
        return value.substring(0, separatorIndex);
    }

    public static String path(Object id) {
        if (id == null) {
            throw new NullPointerException("id");
        }
        String value = id.toString();
        int separatorIndex = value.indexOf(':');
        if (separatorIndex < 0) {
            return value;
        }
        return value.substring(separatorIndex + 1);
    }

    private static Class<?> resolveIdClass() {
        for (String className : new String[]{
                "net.minecraft.resources.Identifier",
                "net.minecraft.resources.ResourceLocation",
                // Fabric production (intermediary): Identifier/ResourceLocation
                "net.minecraft.class_2960",
                // Yarn-named dev environments
                "net.minecraft.util.Identifier",
        }) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException ignored) {
            }
        }
        throw new IllegalStateException("No compatible resource id class found");
    }

    private static Constructor<?> resolveIdConstructor() {
        try {
            Constructor<?> ctor = ID_CLASS.getDeclaredConstructor(String.class, String.class);
            ctor.setAccessible(true);
            return ctor;
        } catch (Exception e) {
            throw new IllegalStateException("No compatible resource id (namespace,path) constructor found: " + ID_CLASS.getName(), e);
        }
    }
}

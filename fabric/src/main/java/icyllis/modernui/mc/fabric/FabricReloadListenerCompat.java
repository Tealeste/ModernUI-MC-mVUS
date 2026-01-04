package icyllis.modernui.mc.fabric;

import icyllis.modernui.mc.ModernUIMod;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

final class FabricReloadListenerCompat {

    private FabricReloadListenerCompat() {
    }

    @FunctionalInterface
    interface ReloadHandler {
        @Nonnull
        CompletableFuture<Void> reload(@Nonnull PreparableReloadListener.SharedState sharedState,
                                      @Nonnull Executor preparationExecutor,
                                      @Nonnull PreparableReloadListener.PreparationBarrier preparationBarrier,
                                      @Nonnull Executor reloadExecutor);
    }

    static void registerSimpleClientReloadListener(String idPath, Consumer<ResourceManager> onReload) {
        Objects.requireNonNull(idPath, "idPath");
        Objects.requireNonNull(onReload, "onReload");

        Object helper = ResourceManagerHelper.get(PackType.CLIENT_RESOURCES);
        Object listener = createProxy(
                "net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener",
                idPath,
                (method, args) -> {
                    // In production Fabric (intermediary), the PreparableReloadListener method name is obfuscated
                    // (e.g. method_25931), so we must not rely on method names here.
                    if (isReloadMethod(method, args)) {
                        final PreparableReloadListener.PreparationBarrier preparationBarrier;
                        final ResourceManager resourceManager;
                        final Executor reloadExecutor;

                        // Newer MC/Fabric: (SharedState, prepExecutor, barrier, reloadExecutor)
                        // Older MC/Fabric: (barrier, ResourceManager, prepExecutor, reloadExecutor)
                        if (args[0] instanceof PreparableReloadListener.SharedState sharedState) {
                            resourceManager = sharedState.resourceManager();
                            preparationBarrier = (PreparableReloadListener.PreparationBarrier) args[2];
                            reloadExecutor = (Executor) args[3];
                        } else {
                            preparationBarrier = (PreparableReloadListener.PreparationBarrier) args[0];
                            resourceManager = (ResourceManager) args[1];
                            reloadExecutor = (Executor) args[3];
                        }

                        return preparationBarrier
                                .wait(CompletableFuture.completedFuture(null))
                                .thenRunAsync(() -> onReload.accept(resourceManager), reloadExecutor);
                    }
                    if ("onResourceManagerReload".equals(method.getName()) && args != null && args.length == 1) {
                        onReload.accept((ResourceManager) args[0]);
                        return null;
                    }
                    return null;
                }
        );
        invokeRegisterReloadListener(helper, listener);
    }

    static void registerIdentifiableClientReloadListener(String idPath, ReloadHandler handler) {
        Objects.requireNonNull(idPath, "idPath");
        Objects.requireNonNull(handler, "handler");

        Object helper = ResourceManagerHelper.get(PackType.CLIENT_RESOURCES);
        Object listener = createProxy(
                "net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener",
                idPath,
                (method, args) -> {
                    if (isReloadMethod(method, args)) {
                        if (!(args[0] instanceof PreparableReloadListener.SharedState)) {
                            throw new IllegalStateException("Unsupported reload signature for " + idPath + ": " + method);
                        }
                        return handler.reload(
                                (PreparableReloadListener.SharedState) args[0],
                                (Executor) args[1],
                                (PreparableReloadListener.PreparationBarrier) args[2],
                                (Executor) args[3]
                        );
                    }
                    return null;
                }
        );
        invokeRegisterReloadListener(helper, listener);
    }

    private static boolean isReloadMethod(Method method, Object[] args) {
        return args != null
                && args.length == 4
                && CompletionStage.class.isAssignableFrom(method.getReturnType());
    }

    private static void invokeRegisterReloadListener(Object helper, Object listener) {
        try {
            Method register = findSingleArgMethod(helper.getClass(), "registerReloadListener");
            register.invoke(helper, listener);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register Fabric reload listener", e);
        }
    }

    private static Method findSingleArgMethod(Class<?> owner, String name) {
        for (Method method : owner.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 1) {
                return method;
            }
        }
        throw new IllegalStateException("No compatible " + owner.getName() + "#" + name + "(...) overload found");
    }

    private interface ExtraInvocationHandler {
        Object handle(Method method, Object[] args) throws Exception;
    }

    private static Object createProxy(String listenerInterfaceName, String idPath, ExtraInvocationHandler extraHandler) {
        try {
            Class<?> listenerInterface = Class.forName(listenerInterfaceName);
            return Proxy.newProxyInstance(
                    listenerInterface.getClassLoader(),
                    new Class<?>[]{listenerInterface},
                    (proxy, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            return switch (method.getName()) {
                                case "toString" -> listenerInterfaceName + "(" + idPath + ")";
                                case "hashCode" -> System.identityHashCode(proxy);
                                case "equals" -> proxy == (args != null && args.length == 1 ? args[0] : null);
                                default -> null;
                            };
                        }
                        if ("getFabricId".equals(method.getName()) && method.getParameterCount() == 0) {
                            return ModernUIMod.location(idPath);
                        }
                        Object result = extraHandler.handle(method, args);
                        if (result != null) {
                            return result;
                        }
                        if (isReloadMethod(method, args)) {
                            throw new IllegalStateException("Unhandled reload method for " + listenerInterfaceName + "(" + idPath + "): " + method);
                        }
                        if ("getFabricDependencies".equals(method.getName()) && method.getParameterCount() == 0) {
                            return java.util.List.of();
                        }
                        return null;
                    }
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Fabric reload listener proxy for " + listenerInterfaceName, e);
        }
    }
}

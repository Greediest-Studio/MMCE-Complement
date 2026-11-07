package net.edwin.mmcecomplement.compat.ae.tile;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.IntConsumer;

/**
 * Bridges the two listener APIs shipped by Modular Machinery CE releases.
 *
 * <p>Releases use {@code InventoryUpdateListener#onChange()}, the JDK
 * {@code IntConsumer}, or MMCE's own {@code IntConsumer#accept}. Calling
 * either overload directly would put that method descriptor in the constant
 * pool and cause a {@link NoSuchMethodError} with another release.</p>
 *
 * <p>The accept adapter and proxy identity handling incorporate Butterfly-Kiyo's
 * fix from Greediest-Studio/MMCE-Complement PR #1 (9c6a5ac611b65dafa7dc7caa9dc883be1a11315e).
 * The older no-slot onChange callback is retained.</p>
 */
final class IOInventoryListenerCompat {

    private IOInventoryListenerCompat() {
    }

    static void setListener(Object inventory, IntConsumer listener) {
        try {
            Method setter = findSetter(inventory.getClass());
            Class<?> listenerType = setter.getParameterTypes()[0];
            Object adapted = listenerType.isInstance(listener) ? listener : adapt(listenerType, listener);
            setter.invoke(inventory, adapted);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Could not install MMCE inventory listener", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unsupported MMCE inventory listener API", e);
        }
    }

    private static Method findSetter(Class<?> inventoryType) throws NoSuchMethodException {
        try {
            return inventoryType.getMethod("setListener", IntConsumer.class);
        } catch (NoSuchMethodException customListener) {
            // Older MMCE releases use a nested listener interface.
        }
        for (Method method : inventoryType.getMethods()) {
            if ("setListener".equals(method.getName())
                && method.getParameterTypes().length == 1) {
                return method;
            }
        }
        throw new NoSuchMethodException(inventoryType.getName() + ".setListener");
    }

    private static Object adapt(Class<?> listenerType, IntConsumer listener) {
        if (!listenerType.isInterface()) {
            throw new IllegalStateException("Unsupported MMCE listener type: " + listenerType.getName());
        }
        return Proxy.newProxyInstance(listenerType.getClassLoader(), new Class<?>[]{listenerType}, (proxy, method, args) -> {
            if ("equals".equals(method.getName())) return proxy == args[0];
            if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
            if ("toString".equals(method.getName())) return "MMCE inventory listener";
            if ("onChange".equals(method.getName()) || "accept".equals(method.getName())) {
                int slot = -1;
                if (args != null && args.length > 0 && args[0] instanceof Number) {
                    slot = ((Number) args[0]).intValue();
                }
                listener.accept(slot);
                return null;
            }
            throw new UnsupportedOperationException(method.toString());
        });
    }
}

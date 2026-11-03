package net.edwin.mmcecomplement.compat.ae.tile;

import hellfirepvp.modularmachinery.common.util.IOInventory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.IntConsumer;

/**
 * Bridges the two listener APIs shipped by Modular Machinery CE releases.
 *
 * <p>Older releases use {@code InventoryUpdateListener#onChange()}, while
 * newer releases use {@code IntConsumer} and report the changed slot. Calling
 * either overload directly would put that method descriptor in the constant
 * pool and cause a {@link NoSuchMethodError} with the other release.</p>
 */
final class IOInventoryListenerCompat {

    private IOInventoryListenerCompat() {
    }

    static void setListener(IOInventory inventory, IntConsumer listener) {
        Method setter = findSetter();
        if (setter == null) {
            return;
        }

        Class<?> listenerType = setter.getParameterTypes()[0];
        Object adapted = listenerType.isAssignableFrom(IntConsumer.class)
            ? listener
            : adapt(listenerType, listener);
        if (adapted == null) {
            return;
        }

        try {
            setter.invoke(inventory, adapted);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // A mismatched optional MMCE API must not prevent the tile from
            // being constructed. The inventory remains usable without the
            // display update callback.
        }
    }

    private static Method findSetter() {
        for (Method method : IOInventory.class.getMethods()) {
            if ("setListener".equals(method.getName())
                && method.getParameterTypes().length == 1) {
                return method;
            }
        }
        return null;
    }

    private static Object adapt(Class<?> listenerType, IntConsumer listener) {
        if (!listenerType.isInterface()) {
            return null;
        }
        InvocationHandler handler = (proxy, method, args) -> {
            if ("onChange".equals(method.getName())) {
                int slot = -1;
                if (args != null && args.length > 0 && args[0] instanceof Number) {
                    slot = ((Number) args[0]).intValue();
                }
                listener.accept(slot);
            }
            return defaultValue(method.getReturnType());
        };
        return Proxy.newProxyInstance(listenerType.getClassLoader(),
            new Class<?>[]{listenerType}, handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) {
            return null;
        }
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }
}

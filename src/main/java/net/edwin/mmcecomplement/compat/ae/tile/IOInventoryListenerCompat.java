package net.edwin.mmcecomplement.compat.ae.tile;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.IntConsumer;

/** Keeps inventory callbacks compatible with both MMCE listener signatures. */
final class IOInventoryListenerCompat {
    private IOInventoryListenerCompat() { }

    static void setListener(Object inventory, IntConsumer listener) {
        try {
            Method setter;
            Object callback = listener;
            try {
                setter = inventory.getClass().getMethod("setListener", IntConsumer.class);
            } catch (NoSuchMethodException modernSignatureMissing) {
                Class<?> legacyType = Class.forName(
                    "hellfirepvp.modularmachinery.common.util.IOInventory$IntConsumer",
                    false, inventory.getClass().getClassLoader());
                setter = inventory.getClass().getMethod("setListener", legacyType);
                callback = Proxy.newProxyInstance(legacyType.getClassLoader(),
                    new Class<?>[] {legacyType}, (proxy, method, args) -> {
                        if ("accept".equals(method.getName()) && args != null
                            && args.length == 1) {
                            listener.accept(((Number) args[0]).intValue());
                            return null;
                        }
                        if ("equals".equals(method.getName())) return proxy == args[0];
                        if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                        if ("toString".equals(method.getName())) return "MMCE inventory listener";
                        throw new UnsupportedOperationException(method.toString());
                    });
            }
            setter.invoke(inventory, callback);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Could not install MMCE inventory listener", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unsupported MMCE inventory listener API", e);
        }
    }
}

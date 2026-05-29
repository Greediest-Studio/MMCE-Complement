package net.edwin.mmcecomplement.compat.gugu;

import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEManaBusBase;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public final class GuguManaCompatBridge {

    private static final String CLAZZ_GENERATABLE = "com.warmthdawn.mod.gugu_utils.modularmachenary.requirements.basic.IGeneratable";
    private static final String CLAZZ_CONSUMABLE = "com.warmthdawn.mod.gugu_utils.modularmachenary.requirements.basic.IConsumable";
    private static final String CLAZZ_GENERIC_COMPONENT = "com.warmthdawn.mod.gugu_utils.modularmachenary.components.GenericMachineCompoment";
    private static final String CLAZZ_COMPONENT_HOLDER = "com.warmthdawn.mod.gugu_utils.modularmachenary.MMCompoments";

    private static final boolean AVAILABLE;
    private static final Class<?> CONSUMABLE_CLASS;
    private static final Class<?> GENERATABLE_CLASS;
    private static final Constructor<?> CONSUMABLE_CTOR;
    private static final Constructor<?> GENERATABLE_CTOR;
    private static final Object COMPONENT_MANA;

    static {
        Class<?> consumableClass = null;
        Class<?> generatableClass = null;
        Constructor<?> consumableCtor = null;
        Constructor<?> generatableCtor = null;
        Object componentMana = null;
        boolean available = false;

        try {
            ClassLoader cl = GuguManaCompatBridge.class.getClassLoader();
            consumableClass = Class.forName(CLAZZ_CONSUMABLE, false, cl);
            generatableClass = Class.forName(CLAZZ_GENERATABLE, false, cl);
            Class<?> genericClass = Class.forName(CLAZZ_GENERIC_COMPONENT, false, cl);
            Class<?> compHolderClass = Class.forName(CLAZZ_COMPONENT_HOLDER, false, cl);

            consumableCtor = genericClass.getConstructor(consumableClass,
                    hellfirepvp.modularmachinery.common.crafting.ComponentType.class);
            generatableCtor = genericClass.getConstructor(generatableClass,
                    hellfirepvp.modularmachinery.common.crafting.ComponentType.class);

            Field compManaField = compHolderClass.getField("COMPONENT_MANA");
            componentMana = compManaField.get(null);

            available = componentMana instanceof hellfirepvp.modularmachinery.common.crafting.ComponentType;
        } catch (Throwable ignored) {
        }

        CONSUMABLE_CLASS = consumableClass;
        GENERATABLE_CLASS = generatableClass;
        CONSUMABLE_CTOR = consumableCtor;
        GENERATABLE_CTOR = generatableCtor;
        COMPONENT_MANA = componentMana;
        AVAILABLE = available;
    }

    private GuguManaCompatBridge() {}

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    public static MachineComponent createInputComponent(TileMEManaBusBase bus) {
        return createComponent(bus, true);
    }

    public static MachineComponent createOutputComponent(TileMEManaBusBase bus) {
        return createComponent(bus, false);
    }

    @SuppressWarnings("unchecked")
    private static MachineComponent createComponent(TileMEManaBusBase bus, boolean input) {
        if (!AVAILABLE) {
            return null;
        }

        try {
            Class<?> iface = input ? CONSUMABLE_CLASS : GENERATABLE_CLASS;
            InvocationHandler handler = (proxy, method, args) -> invokeBus(bus, method, args, input);
            Object delegate = Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] {iface}, handler);
            Object component = input
                    ? CONSUMABLE_CTOR.newInstance(delegate, COMPONENT_MANA)
                    : GENERATABLE_CTOR.newInstance(delegate, COMPONENT_MANA);
            return (MachineComponent) component;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invokeBus(TileMEManaBusBase bus, Method method, Object[] args, boolean input) throws Exception {
        String name = method.getName();

        if ("toString".equals(name)) {
            return "GuguManaProxy(" + bus.getClass().getSimpleName() + ")";
        }
        if ("hashCode".equals(name)) {
            return System.identityHashCode(bus);
        }
        if ("equals".equals(name)) {
            return args != null && args.length == 1 && args[0] == bus;
        }

        if (("consume".equals(name) || "generate".equals(name)) && args != null && args.length >= 2) {
            Object token = args[0];
            boolean apply = args[1] instanceof Boolean && (Boolean) args[1];

            Method getMana = token.getClass().getMethod("getMana");
            Method setMana = token.getClass().getMethod("setMana", int.class);

            int requested = (Integer) getMana.invoke(token);
            if (requested <= 0) {
                return false;
            }

            long moved = input
                    ? Math.min(requested, bus.getCurrentMana())
                    : Math.min(requested, bus.getRemainingCapacity());

            setMana.invoke(token, (int) (requested - moved));

            if (apply && moved > 0L) {
                if (input) {
                    bus.extractManaToMachine(moved);
                } else {
                    bus.receiveManaFromMachine(moved);
                }
            }

            return moved > 0L;
        }

        return null;
    }
}

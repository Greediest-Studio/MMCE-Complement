package net.edwin.mmcecomplement.mixin;

import com.google.common.collect.HashMultimap;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LateMixinSelectionTest {
    @Test void nativeBridgeWithBothModIdsNeverLoadsUelMixins() throws Exception {
        List<String> configs = configurations("ae2", "appliedenergistics2", "mekeng", "mekanism");
        assertTrue(configs.contains("mixins.mmce_complement.ae2s.json"));
        assertTrue(configs.contains("mixins.mmce_complement.ae2s.mekeng.json"));
        assertTrue(configs.contains("mixins.mmce_complement.mekanism.json"));
        assertFalse(configs.contains("mixins.mmce_complement.ae2uel.json"));
    }

    @Test void uelRetainsItsGuiMixinsWithoutNativeDependencies() throws Exception {
        List<String> configs = configurations("appliedenergistics2");
        assertTrue(configs.contains("mixins.mmce_complement.ae2uel.json"));
        assertFalse(configs.contains("mixins.mmce_complement.ae2s.json"));
        assertFalse(configs.contains("mixins.mmce_complement.ae2s.mekeng.json"));
    }

    @Test void nativeBaseWithoutGasDoesNotLoadGasMixins() throws Exception {
        List<String> configs = configurations("ae2", "appliedenergistics2");
        assertTrue(configs.contains("mixins.mmce_complement.ae2s.json"));
        assertFalse(configs.contains("mixins.mmce_complement.ae2s.mekeng.json"));
        assertFalse(configs.contains("mixins.mmce_complement.ae2uel.json"));
    }

    private static List<String> configurations(String... ids) throws Exception {
        // Forge's discovery list is the actual input to the late-mixin loader.
        // Avoid booting a game or replacing any production branch-selection code.
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        Field access = unsafe.getDeclaredField("theUnsafe");
        access.setAccessible(true);
        Object allocator = access.get(null);
        Loader loader = (Loader) unsafe.getMethod("allocateInstance", Class.class).invoke(allocator, Loader.class);
        LoadController controller = (LoadController) unsafe.getMethod("allocateInstance", Class.class)
            .invoke(allocator, LoadController.class);
        Field states = LoadController.class.getDeclaredField("modStates");
        states.setAccessible(true);
        states.set(controller, HashMultimap.create());
        Map<String, ModContainer> mods = new HashMap<>();
        for (String id : ids) {
            mods.put(id, (ModContainer) Proxy.newProxyInstance(ModContainer.class.getClassLoader(),
                new Class<?>[]{ModContainer.class},
                (proxy, method, args) -> method.getName().equals("getModId") ? id : null));
        }
        Field names = Loader.class.getDeclaredField("namedMods");
        names.setAccessible(true);
        names.set(loader, mods);
        Field control = Loader.class.getDeclaredField("modController");
        control.setAccessible(true);
        control.set(loader, controller);
        Field singleton = Loader.class.getDeclaredField("instance");
        singleton.setAccessible(true);
        Object previous = singleton.get(null);
        try {
            singleton.set(null, loader);
            return new MMCEComplementLateMixinLoader().getMixinConfigs();
        } finally {
            singleton.set(null, previous);
        }
    }
}

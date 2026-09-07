package net.edwin.mmcecomplement.mixin;

import zone.rong.mixinbooter.ILateMixinLoader;
import net.minecraftforge.fml.common.Loader;

import java.util.ArrayList;
import java.util.List;

/** Registers the complement mixins after Forge has discovered MMCE. */
public final class MMCEComplementLateMixinLoader implements ILateMixinLoader {

    @Override
    public List<String> getMixinConfigs() {
        List<String> configs = new ArrayList<>();
        configs.add("mixins.mmce_complement.json");
        if (Loader.isModLoaded("mekanism")) {
            configs.add("mixins.mmce_complement.mekanism.json");
        }
        return configs;
    }
}

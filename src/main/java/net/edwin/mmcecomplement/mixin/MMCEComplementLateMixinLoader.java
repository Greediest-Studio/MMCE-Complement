package net.edwin.mmcecomplement.mixin;

import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Collections;
import java.util.List;

/** Registers the complement mixins after Forge has discovered MMCE. */
public final class MMCEComplementLateMixinLoader implements ILateMixinLoader {

    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.mmce_complement.json");
    }
}

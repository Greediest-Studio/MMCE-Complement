package net.edwin.mmcecomplement.event;

import crafttweaker.mc1120.events.ScriptRunEvent;
import net.edwin.mmcecomplement.Tags;
import net.edwin.mmcecomplement.redstoneinterface.RedstoneInterfaceRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Prevents stale or duplicate definitions when CraftTweaker scripts reload. */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class RedstoneInterfaceScriptEvents {

    private RedstoneInterfaceScriptEvents() {
    }

    @SubscribeEvent
    public static void onScriptsLoading(ScriptRunEvent.Pre event) {
        RedstoneInterfaceRegistry.clear();
    }
}

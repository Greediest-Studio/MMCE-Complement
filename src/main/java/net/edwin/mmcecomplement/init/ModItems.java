package net.edwin.mmcecomplement.init;

import net.edwin.mmcecomplement.item.ItemAttachmentConstructTool;
import net.edwin.mmcecomplement.item.ItemBlockMachineSignalCable;
import net.minecraft.item.Item;

/** Static references to items provided by MMCE Complement. */
public final class ModItems {

    public static ItemAttachmentConstructTool ATTACHMENT_CONSTRUCT_TOOL;
    // Keep the optional integration item on the vanilla type so this holder
    // remains loadable when MMCE Addons is not installed.
    public static Item MECHANICAL_BINDING_TOOL;
    /** Separate registry entries for the uncolored and sixteen dyed cable items. */
    public static ItemBlockMachineSignalCable[] MACHINE_SIGNAL_CABLE_ITEMS;

    private ModItems() {
    }
}

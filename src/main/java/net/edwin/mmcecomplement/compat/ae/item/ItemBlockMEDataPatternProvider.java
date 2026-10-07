package net.edwin.mmcecomplement.compat.ae.item;

import hellfirepvp.modularmachinery.common.item.ItemBlockMEMachineComponent;
import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

/** ItemBlock with a stable display-name lookup for data pattern providers. */
public class ItemBlockMEDataPatternProvider
    extends ItemBlockMEMachineComponent {

    public ItemBlockMEDataPatternProvider(Block block) {
        super(block);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return I18n.format(getBlock().getTranslationKey() + ".name").trim();
    }
}

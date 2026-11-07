package net.edwin.mmcecomplement.compat.ae2s;

import ae2.api.crafting.IAssemblerPattern;
import ae2.api.crafting.IPatternDetails;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.GenericStack;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import java.util.ArrayList;
import java.util.List;

/** Pattern identity for Complement's work modes and data markers. */
final class ComplementPatternDetails implements ICraftingPatternDetails {
    private final IPatternDetails pattern;
    private int priority;
    ComplementPatternDetails(IPatternDetails pattern) { this.pattern = pattern; }
    @Override public boolean equals(Object other) {
        return this == other || other instanceof ComplementPatternDetails
            && pattern.equals(((ComplementPatternDetails) other).pattern);
    }
    @Override public int hashCode() { return pattern.hashCode(); }
    @Override public ItemStack getPattern() { return pattern.getDefinition().toStack(); }
    @Override public boolean isCraftable() { return pattern instanceof IAssemblerPattern; }
    @Override public boolean isValidItemForSlot(int slot, ItemStack item, World world) {
        return slot >= 0 && slot < pattern.getInputs().length && !item.isEmpty()
            && pattern.getInputs()[slot].isValid(AEItemKey.of(item), world);
    }
    @Override public IAEItemStack[] getInputs() {
        List<GenericStack> stacks = new ArrayList<>();
        for (IPatternDetails.IInput input : pattern.getInputs()) {
            GenericStack option = input.possibleInputs()[0];
            stacks.add(new GenericStack(option.what(), Math.multiplyExact(option.amount(), input.getMultiplier())));
        }
        return items(stacks);
    }
    @Override public IAEItemStack[] getOutputs() { return items(pattern.getOutputs()); }
    @Override public IAEItemStack[] getCondensedInputs() { return getInputs(); }
    @Override public IAEItemStack[] getCondensedOutputs() { return getOutputs(); }
    @Override public boolean canSubstitute() { return false; }
    @Override public ItemStack getOutput(InventoryCrafting inventory, World world) { return getOutputs()[0].createItemStack(); }
    @Override public int getPriority() { return priority; }
    @Override public void setPriority(int priority) { this.priority = priority; }
    private static IAEItemStack[] items(List<GenericStack> stacks) {
        IAEItemStack[] result = new IAEItemStack[stacks.size()];
        for (int i = 0; i < stacks.size(); i++) {
            GenericStack stack = stacks.get(i);
            ItemStack item = stack.what() instanceof AEItemKey ? ((AEItemKey) stack.what()).toStack()
                : GenericStack.wrapInItemStack(stack.what(), 1);
            result[i] = AEItemStack.fromItemStack(item).setStackSize(stack.amount());
        }
        return result;
    }
}

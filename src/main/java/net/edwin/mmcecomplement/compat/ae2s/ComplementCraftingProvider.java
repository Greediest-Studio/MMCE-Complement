package net.edwin.mmcecomplement.compat.ae2s;

import ae2.api.crafting.IAssemblerPattern;
import ae2.api.crafting.IPatternDetails;
import ae2.api.crafting.PatternDetailsHelper;
import ae2.api.implementations.blockentities.PatternContainerGroup;
import ae2.api.networking.crafting.ICraftingProvider;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.GenericStack;
import ae2.api.stacks.KeyCounter;
import ae2.helpers.patternprovider.PatternContainer;
import github.formlessdragon.appcompat.bridge.mmce.PatternProviderCacheExtensions;
import github.kasuminova.mmce.common.tile.MEPatternProvider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Registers only Complement providers on MMCE's existing native node. */
final class ComplementCraftingProvider implements ICraftingProvider {
    private final MEPatternProvider tile;
    private final List<ItemStack> decodedFrom = new ArrayList<>();
    private final List<IPatternDetails> decoded = new ArrayList<>();
    ComplementCraftingProvider(MEPatternProvider tile) { this.tile = tile; }
    @Override public List<IPatternDetails> getAvailablePatterns() {
        List<IPatternDetails> result = new ArrayList<>();
        if (!AE2SNetwork.mainNode(tile).isOnline()) return result;
        int slots = tile.getPatterns().getSlots();
        while (decoded.size() < slots) { decoded.add(null); decodedFrom.add(ItemStack.EMPTY); }
        for (int i = 0; i < slots; i++) {
            ItemStack stack = tile.getPatterns().getStackInSlot(i);
            if (!ItemStack.areItemStacksEqual(stack, decodedFrom.get(i))) {
                IPatternDetails pattern = stack.isEmpty() ? null : PatternDetailsHelper.decodePattern(stack, tile.getWorld());
                decoded.set(i, pattern instanceof IAssemblerPattern ? null : pattern);
                decodedFrom.set(i, stack.copy());
            }
            if (decoded.get(i) != null) result.add(decoded.get(i));
        }
        return result;
    }
    private boolean contains(IPatternDetails pattern) {
        if (pattern == null || pattern instanceof IAssemblerPattern) return false;
        AEItemKey key = pattern.getDefinition();
        for (int i = 0; i < tile.getPatterns().getSlots(); i++) {
            if (key.matches(tile.getPatterns().getStackInSlot(i))) return true;
        }
        return false;
    }
    @Override public boolean pushPattern(IPatternDetails pattern, KeyCounter[] inputs, int multiplier) {
        if (multiplier <= 0 || isBusy() || !AE2SNetwork.mainNode(tile).isActive() || !contains(pattern)
            || !PatternProviderCacheExtensions.supportsInputs(inputs)) return false;
        List<ItemStack> stacks = new ArrayList<>();
        for (KeyCounter counter : inputs) counter.forEach((key, amount) -> {
            if (amount > 0) stacks.add(GenericStack.wrapInItemStack(key, amount));
        });
        InventoryCrafting table = new InventoryCrafting(new Container() {
            @Override public boolean canInteractWith(EntityPlayer player) { return false; }
        }, Math.max(1, stacks.size()), 1);
        for (int i = 0; i < stacks.size(); i++) table.setInventorySlotContents(i, stacks.get(i));
        boolean pushed = tile.pushPattern(new ComplementPatternDetails(pattern), table);
        if (pushed) for (KeyCounter counter : inputs) counter.clear();
        return pushed;
    }
    @Override public boolean canMergePatternPush(IPatternDetails pattern) {
        return !isBusy() && AE2SNetwork.mainNode(tile).isActive() && contains(pattern)
            && tile.getWorkMode() != MEPatternProvider.WorkModeSetting.CRAFTING_LOCK_MODE;
    }
    @Override public int getMaxPatternPushMultiplier(IPatternDetails pattern, int maxMultiplier) {
        return canMergePatternPush(pattern) ? Math.max(0, maxMultiplier) : 0;
    }
    @Override public boolean isBusy() { return tile.isBusy(); }
    @Override public PatternContainerGroup getTerminalGroup() { return ((PatternContainer) tile).getTerminalGroup(); }
}

package net.edwin.mmcecomplement.compat.ae.tile;

import github.kasuminova.mmce.common.event.machine.MachineEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeCheckEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeTickEvent;
import github.kasuminova.mmce.common.tile.base.MEMachineComponent;
import appeng.api.networking.GridFlags;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTileNotifiable;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.edwin.mmcecomplement.mechannel.ModMEChannelTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

/** AE endpoint which shares its network with otherwise unconnected ME hatches. */
public class TileMEConnectionShareHatch extends MEMachineComponent
    implements MachineComponentTileNotifiable {

    public TileMEConnectionShareHatch() {
        // The share endpoint itself is a dense-capacity node and therefore
        // never consumes a channel.  The hatches it replaces still do.
        proxy.setFlags(GridFlags.DENSE_CAPACITY);
    }

    @Override
    public void gridChanged() {
        super.gridChanged();
        MEConnectionShareManager.markDirty(this);
    }

    @Nonnull
    @Override
    public AECableType getCableConnectionType(@Nonnull AEPartLocation dir) {
        return AECableType.DENSE_SMART;
    }

    @Override
    public ItemStack getVisualItemStack() {
        if (ModBlocks.ME_CONNECTION_SHARE_HATCH == null) return ItemStack.EMPTY;
        Item item = Item.getItemFromBlock(ModBlocks.ME_CONNECTION_SHARE_HATCH);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public MachineComponent<Object> provideComponent() {
        return new MachineComponent<Object>(IOType.INPUT) {
            @Override public ComponentType getComponentType() {
                return ModMEChannelTypes.SHARE_COMPONENT;
            }
            @Override public Object getContainerProvider() {
                return TileMEConnectionShareHatch.this;
            }
            @Override public boolean isAsyncSupported() { return true; }
        };
    }

    @Override
    public void onMachineEvent(MachineEvent event) {
        MEConnectionShareManager.syncDuplicateChannels(event.getController());
        String failure = MEConnectionShareManager.failure(event.getController());
        if (failure == null) return;
        if (event instanceof RecipeCheckEvent) {
            ((RecipeCheckEvent) event).setFailed(failure);
        } else if (event instanceof RecipeTickEvent
            && ((RecipeTickEvent) event).phase == github.kasuminova.mmce.common.event.Phase.START) {
            ((RecipeTickEvent) event).preventProgressing(failure);
        }
    }
}

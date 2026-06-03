package net.edwin.mmcecomplement.tile;

import github.kasuminova.mmce.common.event.Phase;
import github.kasuminova.mmce.common.event.machine.MachineEvent;
import github.kasuminova.mmce.common.event.recipe.FactoryRecipeTickEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeTickEvent;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTileNotifiable;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.init.ModComponentTypes;

import javax.annotation.Nonnull;

public class TileMachineControlInterface extends TileColorableMachineComponent implements MachineComponentTile, MachineComponentTileNotifiable {

    private static final String STOP_REASON = "error.mmce_complement.machine_control_interface.powered";

    private final MachineControlInterfaceProvider provider = new MachineControlInterfaceProvider();

    @Nonnull
    @Override
    public MachineControlInterfaceProvider provideComponent() {
        return provider;
    }

    public boolean isRedstonePowered() {
        if (world == null || isInvalid()) {
            return false;
        }
        return world.isBlockPowered(pos)
                || world.getRedstonePowerFromNeighbors(pos) > 0
            || world.getStrongPower(pos) > 0;
    }

    @Override
    public void onMachineEvent(final MachineEvent event) {
        boolean powered = isRedstonePowered();
        if (!powered) {
            return;
        }

        if (event instanceof RecipeTickEvent) {
            RecipeTickEvent recipeTickEvent = (RecipeTickEvent) event;
            if (recipeTickEvent.phase == Phase.START) {
                MMCEComplement.LOGGER.info("[MachineControlInterface] Blocking machine recipe tick at {}", pos);
                recipeTickEvent.preventProgressing(STOP_REASON);
                return;
            }
        }

        if (event instanceof FactoryRecipeTickEvent) {
            FactoryRecipeTickEvent factoryRecipeTickEvent = (FactoryRecipeTickEvent) event;
            if (factoryRecipeTickEvent.phase == Phase.START) {
                MMCEComplement.LOGGER.info("[MachineControlInterface] Blocking factory recipe tick at {}", pos);
                factoryRecipeTickEvent.preventProgressing(STOP_REASON);
            }
            return;
        }
    }

    public class MachineControlInterfaceProvider extends MachineComponent<TileMachineControlInterface> {

        private MachineControlInterfaceProvider() {
            super(IOType.INPUT);
        }

        @Override
        public ComponentType getComponentType() {
            return ModComponentTypes.MACHINE_CONTROL_INTERFACE;
        }

        @Override
        public TileMachineControlInterface getContainerProvider() {
            return TileMachineControlInterface.this;
        }
    }
}
package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.TileSmartInterface;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceData;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceType;
import net.edwin.mmcecomplement.compat.ae.tile.IMEDataPatternProvider;
import net.edwin.mmcecomplement.tile.TileDataItemInputHatch;
import net.edwin.mmcecomplement.tile.TileItemInputAssemblyHatch;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Registers the data half after MMCE registers the hatch's item component. */
@Mixin(value = TileMultiblockMachineController.class, remap = false)
public abstract class MixinDataItemInputController {

    @Inject(method = "checkAndAddComponents", at = @At("RETURN"))
    private void mmceComplement$registerDataItemInterface(
        BlockPos relativePos, BlockPos controllerPos,
        Map<Long, Map<TileEntity, ProcessingComponent<?>>> found,
        CallbackInfo ci) {
        TileMultiblockMachineController controller =
            (TileMultiblockMachineController) (Object) this;
        BlockPos realPos = controllerPos.add(relativePos);
        if (!controller.getWorld().isBlockLoaded(realPos)) {
            return;
        }
        TileEntity tile = controller.getWorld().getTileEntity(realPos);
        if (tile instanceof IMEDataPatternProvider) {
            IMEDataPatternProvider dataProvider =
                (IMEDataPatternProvider) tile;
            TileSmartInterface.SmartInterfaceProvider primary =
                dataProvider.getPrimaryDataProvider();

            /*
             * This block is one physical smart interface even in isolated
             * pattern mode. Registering all 36 group wrappers here is not
             * valid: MMCE filters an interface type out after the first
             * provider claims it, then removes our shared binding while it
             * examines the second provider. The per-slot wrappers are added
             * to RecipeCraftingContext separately, where their group ids are
             * actually needed. Controller binding must therefore use only
             * the hatch-wide primary provider.
             */
            DynamicMachine machine = controller.getFoundMachine();
            if (machine == null || machine.smartInterfaceTypesIsEmpty()) {
                return;
            }
            Map<TileSmartInterface.SmartInterfaceProvider, String>
                registered = controller.getFoundSmartInterfaces();
            List<String> usedByOtherInterfaces = new ArrayList<>();
            for (Map.Entry<TileSmartInterface.SmartInterfaceProvider, String>
                entry : registered.entrySet()) {
                if (!dataProvider.getAllDataProviders()
                    .contains(entry.getKey())) {
                    usedByOtherInterfaces.add(entry.getValue());
                }
            }
            Map<String, SmartInterfaceType> available =
                machine.getFilteredType(usedByOtherInterfaces);
            // Rebuild this physical interface's cache entry from scratch;
            // old versions may also have left per-slot wrappers behind.
            for (TileSmartInterface.SmartInterfaceProvider own
                : dataProvider.getAllDataProviders()) {
                registered.remove(own);
            }
            String selectedType = dataProvider.getSelectedInterfaceType();
            if (selectedType != null) {
                SmartInterfaceType selected =
                    machine.getSmartInterfaceType(selectedType);
                if (selected == null) {
                    /*
                     * A dropped provider carries its shared selection but no
                     * controller binding. If the new machine does not define
                     * that class, allow a fresh automatic selection only when
                     * no other controller binding would be changed.
                     */
                    if (!dataProvider
                        .resetUnsupportedSelectionForController(
                            controller.getPos())) {
                        return;
                    }
                    selectedType = null;
                } else {
                    SmartInterfaceData current =
                        primary.getMachineData(controller.getPos());
                    if (current == null
                        || !selectedType.equals(current.getType())) {
                        /*
                         * Keep a pending association when the persisted type
                         * is supported but currently occupied. It is not
                         * exposed to recipes until the primary provider is
                         * successfully registered below.
                         */
                        primary.addMachineData(controller.getPos(),
                            machine.getRegistryName(), selectedType,
                            current == null ? selected.getDefaultValue()
                                : current.getValue(), true);
                    }
                    if (!available.containsKey(selectedType)) {
                        return;
                    }
                }
            }
            if (selectedType == null && available.isEmpty()) {
                // MMCE falls back to the machine's first type when none is
                // free, which would create a duplicate interface class.
                return;
            }

            controller.checkAndAddSmartInterface(primary, realPos);
            SmartInterfaceData binding =
                primary.getMachineData(controller.getPos());
            String registeredType = registered.get(primary);
            if (binding == null
                || !binding.getType().equals(registeredType)) {
                // Do not let stale/pending boundData participate in recipes.
                registered.remove(primary);
            }
            return;
        }
        if (tile instanceof TileDataItemInputHatch
            && !(tile instanceof TileItemInputAssemblyHatch)) {
            controller.checkAndAddSmartInterface(
                ((TileDataItemInputHatch) tile).getDataProvider(), realPos);
        }
    }
}

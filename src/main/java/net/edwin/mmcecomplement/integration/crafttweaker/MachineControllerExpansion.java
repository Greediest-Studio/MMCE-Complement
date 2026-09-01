package net.edwin.mmcecomplement.integration.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import github.kasuminova.mmce.common.helper.IMachineController;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.edwin.mmcecomplement.attachment.AttachmentController;
import net.edwin.mmcecomplement.attachment.AttachmentMachine;
import net.edwin.mmcecomplement.redstoneinterface.RedstoneDataController;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;
import java.util.Map;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.IGasHandler;
import net.minecraft.util.EnumFacing;

/** CraftTweaker access to the attachment definitions and active state of a controller. */
@ZenRegister
@ZenExpansion("mods.modularmachinery.IMachineController")
public final class MachineControllerExpansion {

    private MachineControllerExpansion() {
    }

    @ZenMethod
    public static boolean hasModule(IMachineController controller, String moduleId) {
        if (controller == null || moduleId == null) {
            return false;
        }
        String normalizedId = moduleId.trim();
        if (normalizedId.isEmpty()) {
            return false;
        }
        TileMultiblockMachineController tile = controller.getController();
        return tile instanceof AttachmentController
            && ((AttachmentController) tile).mmceComplement$isAttachmentModuleActive(normalizedId);
    }

    @ZenGetter("moduleList")
    public static String[] getModuleList(IMachineController controller) {
        if (controller == null || controller.getController() == null) {
            return new String[0];
        }
        DynamicMachine machine = controller.getController().getFoundMachine();
        if (machine == null || !(machine instanceof AttachmentMachine)) {
            return new String[0];
        }
        return ((AttachmentMachine) (Object) machine).mmceComplement$getAttachmentModules()
            .keySet().toArray(new String[0]);
    }

    /** Reads one registered value from all matching input hatches. */
    @ZenMethod
    public static int getRedstone(IMachineController controller, String name) {
        if (controller == null || controller.getController() == null
            || name == null) {
            return 0;
        }
        TileMultiblockMachineController tile = controller.getController();
        return tile instanceof RedstoneDataController
            ? ((RedstoneDataController) tile).mmceComplement$getRedstone(name.trim())
            : 0;
    }

    /** Sets one registered output value for the current controller event tick. */
    @ZenMethod
    public static void setRedstone(IMachineController controller, String name,
                                   int value) {
        if (controller == null || controller.getController() == null
            || name == null) {
            return;
        }
        TileMultiblockMachineController tile = controller.getController();
        if (tile instanceof RedstoneDataController) {
            ((RedstoneDataController) tile)
                .mmceComplement$setRedstone(name.trim(), value);
        }
    }

    /* request input/output extensions intentionally omitted. */
    /*
        Object value = unwrap(stack);
        if (!(value instanceof ItemStack) || ((ItemStack) value).isEmpty()
            || controller == null || controller.getController() == null) return false;
        ItemStack requested = ((ItemStack) value).copy();
        for (Object entry : controller.getController().getGeneralComponents().values()) {
            Object component = unwrapProvider(entry);
            if (!(component instanceof IItemHandler)) continue;
            IItemHandler handler = (IItemHandler) component;
            ItemStack remainder = requested.copy();
            for (int i = 0; i < handler.getSlots() && !remainder.isEmpty(); i++) {
                ItemStack simulated = handler.extractItem(i, remainder.getCount(), false);
                if (!simulated.isEmpty() && simulated.isItemEqual(remainder)
                    && ItemStack.areItemStackTagsEqual(simulated, remainder)) {
                    remainder.shrink(simulated.getCount());
                }
            }
            if (remainder.isEmpty()) {
                int left = requested.getCount();
                for (int i = 0; i < handler.getSlots() && left > 0; i++) {
                    ItemStack taken = handler.extractItem(i, left, true);
                    if (!taken.isEmpty()) left -= taken.getCount();
                }
                if (left == 0) return true;
            }
        }
        return false;
    }

    @ZenMethod
    public static boolean requestFluidInput(IMachineController controller, Object stack) {
        Object value = unwrap(stack);
        if (!(value instanceof FluidStack) || controller == null
            || controller.getController() == null) return false;
        FluidStack requested = ((FluidStack) value).copy();
        for (Object entry : controller.getController().getGeneralComponents().values()) {
            Object component = unwrapProvider(entry);
            if (!(component instanceof IFluidHandler)) continue;
            IFluidHandler handler = (IFluidHandler) component;
            if (handler.fill(requested, false) >= requested.amount
                && handler.drain(requested, false) != null) {
                handler.drain(requested, true);
                return true;
            }
        }
        return false;
    }

    @ZenMethod
    public static boolean requestGasInput(IMachineController controller, Object stack) {
        Object value = unwrap(stack);
        if (!(value instanceof GasStack) || controller == null) return false;
        GasStack request = ((GasStack) value).copy();
        for (Object entry : controller.getController().getGeneralComponents().values()) {
            Object handler = unwrapProvider(entry);
            if (handler instanceof IGasHandler
                && ((IGasHandler) handler).drawGas(null, request.amount, false) != null
                && ((IGasHandler) handler).drawGas(null, request.amount, false).isGasEqual(request)) {
                ((IGasHandler) handler).drawGas(null, request.amount, true);
                return true;
            }
        }
        return false;
    }

    @ZenMethod public static boolean requestItemOutput(IMachineController c, Object s) {
        return requestItemOutput(c, s, false);
    }
    @ZenMethod public static boolean requestItemOutput(IMachineController c, Object s, boolean ignoreMissing) {
        ItemStack value = asItem(s);
        if (value == null || c == null || c.getController() == null) return false;
        boolean found = false;
        if (ignoreMissing) {
            ItemStack left = value.copy();
            for (Object entry : c.getController().getGeneralComponents().values()) {
                Object h = unwrapProvider(entry);
                if (!(h instanceof IItemHandler)) continue;
                found = true;
                IItemHandler handler = (IItemHandler) h;
                for (int i = 0; i < handler.getSlots() && !left.isEmpty(); i++)
                    left = handler.insertItem(i, left, false);
                if (left.isEmpty()) break;
            }
            return true;
        }
        for (Object entry : c.getController().getGeneralComponents().values()) {
            Object h = unwrapProvider(entry);
            if (!(h instanceof IItemHandler)) continue;
            found = true; IItemHandler handler = (IItemHandler) h;
            ItemStack left = value.copy();
            for (int i = 0; i < handler.getSlots() && !left.isEmpty(); i++) left = handler.insertItem(i, left, true);
            if (left.isEmpty()) {
                left = value.copy();
                for (int i = 0; i < handler.getSlots() && !left.isEmpty(); i++) left = handler.insertItem(i, left, false);
                return left.isEmpty();
            }
        }
        return ignoreMissing;
    }
    @ZenMethod public static boolean requestFluidOutput(IMachineController c, Object s) { return requestFluidOutput(c, s, false); }
    @ZenMethod public static boolean requestFluidOutput(IMachineController c, Object s, boolean ignoreMissing) {
        Object value = unwrap(s);
        if (!(value instanceof FluidStack) || c == null || c.getController() == null) return false;
        FluidStack request = ((FluidStack) value).copy(); boolean found = false;
        for (Object entry : c.getController().getGeneralComponents().values()) {
            Object h = unwrapProvider(entry); if (!(h instanceof IFluidHandler)) continue; found = true;
            IFluidHandler handler = (IFluidHandler) h;
            if (handler.fill(request, false) >= request.amount) { handler.fill(request, true); return true; }
            if (ignoreMissing) {
                int accepted = handler.fill(request, true);
                if (accepted >= request.amount) return true;
                if (accepted > 0) { request.amount -= accepted; if (request.amount <= 0) return true; }
            }
        }
        return ignoreMissing;
    }
    @ZenMethod public static boolean requestGasOutput(IMachineController c, Object s) { return requestGasOutput(c, s, false); }
    @ZenMethod public static boolean requestGasOutput(IMachineController c, Object s, boolean ignoreMissing) {
        Object value = unwrap(s); if (!(value instanceof GasStack) || c == null || c.getController() == null) return false;
        GasStack request = ((GasStack) value).copy(); boolean found = false;
        for (Object entry : c.getController().getGeneralComponents().values()) {
            Object h = unwrapProvider(entry); if (!(h instanceof IGasHandler)) continue; found = true;
            IGasHandler handler = (IGasHandler) h;
            if (handler.receiveGas(null, request, false) >= request.amount) { handler.receiveGas(null, request, true); return true; }
            if (ignoreMissing) {
                int accepted = handler.receiveGas(null, request, true);
                if (accepted >= request.amount) return true;
                request.amount -= Math.max(0, accepted);
                if (request.amount <= 0) return true;
            }
        }
        return ignoreMissing;
    }

    private static ItemStack asItem(Object value) {
        Object internal = unwrap(value);
        return internal instanceof ItemStack && !((ItemStack) internal).isEmpty() ? ((ItemStack) internal).copy() : null;
    }

    private static Object unwrap(Object value) {
        if (value == null) return null;
        try {
            Method method = value.getClass().getMethod("getInternal");
            return method.invoke(value);
        } catch (ReflectiveOperationException ignored) {
            return value;
        }
    }

    private static Object unwrapProvider(Object value) {
        try {
            Method method = value.getClass().getMethod("getContainerProvider");
            return method.invoke(value);
        } catch (ReflectiveOperationException ignored) {
            return value;
        }
    }
    */
}

package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.block.BlockFactoryController;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.registry.RegistryBlocks;
import hellfirepvp.modularmachinery.common.registry.RegistryItems;
import hellfirepvp.modularmachinery.common.CommonProxy;
import net.edwin.mmcecomplement.block.BlockStorageController;
import net.edwin.mmcecomplement.variant.StorageMachine;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.objectweb.asm.Opcodes;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Replaces storage controllers before MMCE submits them to its registry primer. */
@Mixin(value = RegistryBlocks.class, remap = false)
public abstract class MixinRegistryBlocks {
    private static DynamicMachine mmceComplement$factoryMachine;
    private static final Map<DynamicMachine, BlockController>
        MMCE_COMPLEMENT_STORAGE_BLOCKS = new IdentityHashMap<>();

    private static boolean isStorage(Block block) {
        if (!(block instanceof BlockController)) return false;
        DynamicMachine machine = ((BlockController) block).getParentMachine();
        return machine instanceof StorageMachine
            && ((StorageMachine) (Object) machine)
                .mmceComplement$isStorageVariant();
    }

    private static BlockController storageBlock(Block block) {
        if (!isStorage(block)) return null;
        // Factory/integrated controllers are deliberately not part of the
        // storage variant.  Returning null leaves the original object for the
        // registration hook, where it is filtered before entering MMCE's
        // registry primer.
        if (block instanceof BlockFactoryController) return null;
        DynamicMachine machine = ((BlockController) block).getParentMachine();
        BlockController result = MMCE_COMPLEMENT_STORAGE_BLOCKS.get(machine);
        if (result == null) {
            result = new BlockStorageController(machine);
            MMCE_COMPLEMENT_STORAGE_BLOCKS.put(machine, result);
        }
        return result;
    }

    /**
     * Storage variants intentionally have one controller type.  MMCE still
     * enters its factory branch when factory controllers are enabled by
     * default, so stop that branch at the registry primer before it can expose
     * an integrated controller block or item.  The ordinary controller branch
     * below remains registered normally.
     */
    @org.spongepowered.asm.mixin.injection.Inject(
        method = "prepareRegisterWithCustomName", at = @At("HEAD"),
        cancellable = true)
    private static void mmceComplement$skipStorageFactoryBlock(Block block,
        CallbackInfoReturnable<Block> cir) {
        if (block instanceof BlockFactoryController && isStorage(block)) {
            cir.setReturnValue(block);
        }
    }

    @org.spongepowered.asm.mixin.injection.Inject(
        method = "prepareItemBlockRegisterWithCustomName", at = @At("HEAD"),
        cancellable = true)
    private static void mmceComplement$skipStorageFactoryItem(
        net.minecraft.item.ItemBlock item,
        CallbackInfoReturnable<net.minecraft.item.ItemBlock> cir) {
        if (item != null && item.getBlock() instanceof BlockFactoryController
            && isStorage(item.getBlock())) {
            cir.setReturnValue(item);
        }
    }

    @ModifyArg(method = "registerCustomControllers",
        at = @At(value = "INVOKE", target =
            "Lhellfirepvp/modularmachinery/common/registry/RegistryBlocks;"
                + "prepareRegisterWithCustomName(Lnet/minecraft/block/Block;)"
                + "Lnet/minecraft/block/Block;"), index = 0)
    private static Block mmceComplement$replaceStorageController(Block block) {
        BlockController replacement = storageBlock(block);
        return replacement == null ? block : replacement;
    }

    @Redirect(method = "registerCustomControllers", at = @At(value = "INVOKE",
        target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object mmceComplement$skipStorageFactoryMapEntry(
        Map map, Object key, Object value) {
        if (map == BlockFactoryController.FACTORY_CONTROLLERS
            && key instanceof DynamicMachine
            && key instanceof StorageMachine
            && ((StorageMachine) (Object) key)
                .mmceComplement$isStorageVariant()) {
            return null;
        }
        // Do this at the map boundary as well as at the prepare* call. This
        // covers both the normal and MOC controller registration branches and
        // guarantees the map never retains MMCE's plain controller instance.
        if (map == BlockController.MACHINE_CONTROLLERS
            && value instanceof BlockController) {
            BlockController replacement = storageBlock((Block) value);
            if (replacement != null) value = replacement;
        }
        return map.put(key, value);
    }

    @Redirect(method = "registerCustomControllers", at = @At(value = "INVOKE",
        target = "Lhellfirepvp/modularmachinery/common/machine/DynamicMachine;"
            + "isHasFactory()Z"))
    private static boolean mmceComplement$disableStorageFactory(
        DynamicMachine machine) {
        mmceComplement$factoryMachine = machine;
        if (isStorageMachine(machine)) return false;
        try {
            java.lang.reflect.Method method = machine.getClass()
                .getMethod("isHasFactory");
            return (Boolean) method.invoke(machine);
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }

    @Redirect(method = "registerCustomControllers", at = @At(value = "FIELD",
        target = "Lhellfirepvp/modularmachinery/common/data/Config;"
            + "enableFactoryControllerByDefault:Z", opcode = Opcodes.GETSTATIC))
    private static boolean mmceComplement$storageFactoryDefault() {
        return mmceComplement$factoryMachine != null
            && isStorageMachine(mmceComplement$factoryMachine)
            ? false
            : hellfirepvp.modularmachinery.common.data.Config
                .enableFactoryControllerByDefault;
    }

    private static boolean isStorageMachine(DynamicMachine machine) {
        return machine instanceof StorageMachine
            && ((StorageMachine) (Object) machine)
                .mmceComplement$isStorageVariant();
    }


    @Inject(method = "registerCustomControllers", at = @At("RETURN"))
    private static void mmceComplement$removeStorageFactoryControllers(
        CallbackInfo ci) {
        for (DynamicMachine machine : MMCE_COMPLEMENT_STORAGE_BLOCKS.keySet()) {
            if (machine instanceof StorageMachine
                && ((StorageMachine) (Object) machine)
                    .mmceComplement$isStorageVariant()) {
                BlockFactoryController.FACTORY_CONTROLLERS.remove(machine);
            }
        }
        mmceComplement$purgeFactoryObjects();
    }

    /**
     * A few MMCE builds populate their internal primer through a path that
     * bypasses the helper injections above. Purge storage factory objects at
     * the end of controller creation as a final registration barrier, before
     * Forge receives either the block or its ItemBlock.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void mmceComplement$purgeFactoryObjects() {
        try {
            java.lang.reflect.Field modelField = RegistryBlocks.class
                .getDeclaredField("blockModelRegister");
            modelField.setAccessible(true);
            mmceComplement$removeFactoryBlocks((List) modelField.get(null));

            if (CommonProxy.registryPrimer != null) {
                java.lang.reflect.Field primedField =
                    CommonProxy.registryPrimer.getClass()
                        .getDeclaredField("primed");
                primedField.setAccessible(true);
                Map primed = (Map) primedField.get(CommonProxy.registryPrimer);
                for (Object value : primed.values()) {
                    if (value instanceof List) {
                        mmceComplement$removeFactoryBlocks((List) value);
                    }
                }
            }

            for (String fieldName : new String[] {
                "ITEM_BLOCKS", "ITEM_BLOCKS_WITH_CUSTOM_NAME",
                "ITEM_MODEL_REGISTER"}) {
                java.lang.reflect.Field itemField = RegistryItems.class
                    .getDeclaredField(fieldName);
                itemField.setAccessible(true);
                List items = (List) itemField.get(null);
                items.removeIf(entry -> entry instanceof ItemBlock
                    && mmceComplement$isStorageFactory(
                        ((ItemBlock) entry).getBlock()));
            }
        } catch (ReflectiveOperationException ignored) {
            // The normal mixin injections still handle supported MMCE builds;
            // this cleanup is only a compatibility barrier for variant builds.
        }
    }

    private static void mmceComplement$removeFactoryBlocks(List entries) {
        entries.removeIf(entry -> entry instanceof Block
            && mmceComplement$isStorageFactory((Block) entry));
    }

    private static boolean mmceComplement$isStorageFactory(Block block) {
        return block instanceof BlockFactoryController && isStorage(block);
    }
}

package net.edwin.mmcecomplement.mixin;

import github.kasuminova.mmce.common.upgrade.MachineUpgrade;
import github.kasuminova.mmce.common.upgrade.SimpleMachineUpgrade;
import github.kasuminova.mmce.common.upgrade.UpgradeType;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.edwin.mmcecomplement.attachment.AttachmentController;
import net.edwin.mmcecomplement.attachment.AttachmentMachine;
import net.edwin.mmcecomplement.attachment.AttachmentModule;
import net.edwin.mmcecomplement.attachment.AttachmentPatternResolver;
import net.edwin.mmcecomplement.attachment.AttachmentResolver;
import net.edwin.mmcecomplement.accelerator.AcceleratorHatchLogic;
import net.edwin.mmcecomplement.block.BlockAcceleratorHatch;
import net.edwin.mmcecomplement.block.BlockOverclockHatch;
import net.edwin.mmcecomplement.batch.BatchController;
import net.edwin.mmcecomplement.config.ModConfig;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.edwin.mmcecomplement.overclock.OverclockHatchLogic;
import net.edwin.mmcecomplement.tile.TileBatchHatch;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(value = TileMultiblockMachineController.class, remap = false)
public abstract class MixinTileMultiblockMachineController implements AttachmentController, BatchController {

    @Unique
    private static final String mmceComplement$NBT_MODULES = "mmceComplementModules";

    @Unique
    private static final String mmceComplement$OVERCLOCK_ENERGY_MODIFIER =
        "mmce_complement:overclock_hatch_energy";

    @Unique
    private static final String mmceComplement$OVERCLOCK_DURATION_MODIFIER =
        "mmce_complement:overclock_hatch_duration";

    @Unique
    private static final String mmceComplement$ACCELERATOR_DURATION_MODIFIER =
        "mmce_complement:accelerator_hatch_duration";

    @Shadow
    protected DynamicMachine foundMachine;

    @Shadow
    protected EnumFacing controllerRotation;

    @Shadow
    protected TaggedPositionBlockArray foundPattern;

    @Shadow
    protected DynamicMachine.ModifierReplacementMap foundReplacements;

    @Shadow
    protected int lastStructureCheckTick;

    @Shadow
    protected Map<String, List<MachineUpgrade>> foundUpgrades;

    @Unique
    private final Set<String> mmceComplement$activeModules = new LinkedHashSet<>();

    @Unique
    private final Map<String, MachineUpgrade> mmceComplement$moduleUpgrades = new LinkedHashMap<>();

    @Unique
    private TaggedPositionBlockArray mmceComplement$mainPattern;

    @Unique
    private TaggedPositionBlockArray mmceComplement$combinedPattern;

    @Unique
    private int mmceComplement$previousStructureCheckTick = -1;

    @Unique
    private volatile int mmceComplement$maxBatchTime;

    @Override
    public Set<String> mmceComplement$getActiveAttachmentModules() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(mmceComplement$activeModules));
    }

    @Override
    public boolean mmceComplement$isAttachmentModuleActive(String id) {
        return mmceComplement$activeModules.contains(id);
    }

    @Override
    public int mmceComplement$getMaxBatchTime() {
        return mmceComplement$maxBatchTime;
    }

    @Inject(method = "checkStructure", at = @At("HEAD"))
    private void mmceComplement$restoreMainPatternForCheck(CallbackInfoReturnable<Boolean> cir) {
        mmceComplement$previousStructureCheckTick = lastStructureCheckTick;
        if (foundPattern == mmceComplement$combinedPattern) {
            foundPattern = mmceComplement$mainPattern;
            mmceComplement$combinedPattern = null;
        }
    }

    @Inject(method = "checkStructure", at = @At("RETURN"))
    private void mmceComplement$checkAttachmentsAfterMainStructure(
        CallbackInfoReturnable<Boolean> cir) {
        if (lastStructureCheckTick == mmceComplement$previousStructureCheckTick
            || !cir.getReturnValue() || foundMachine == null || foundPattern == null) {
            return;
        }
        TileMultiblockMachineController controller = (TileMultiblockMachineController) (Object) this;
        if (controller.getWorld() == null || controller.getWorld().isRemote) {
            return;
        }
        mmceComplement$mainPattern = foundPattern;
        mmceComplement$refreshModules(controller);
        mmceComplement$combineActivePatterns();
    }

    @Inject(method = "updateComponents", at = @At("RETURN"))
    private void mmceComplement$finishAttachmentComponents(CallbackInfo ci) {
        mmceComplement$rebuildSyntheticUpgrades();
        mmceComplement$refreshOverclockHatches(
            (TileMultiblockMachineController) (Object) this);
        mmceComplement$refreshAcceleratorHatches(
            (TileMultiblockMachineController) (Object) this);
        mmceComplement$refreshBatchHatches(
            (TileMultiblockMachineController) (Object) this);
    }

    @Inject(method = "resetMachine", at = @At("RETURN"))
    private void mmceComplement$resetAttachmentModules(boolean clearData, CallbackInfo ci) {
        mmceComplement$mainPattern = null;
        mmceComplement$combinedPattern = null;
        mmceComplement$replaceActiveModules(Collections.emptySet(), true);
        mmceComplement$clearOverclockModifiers(
            (TileMultiblockMachineController) (Object) this);
        mmceComplement$clearAcceleratorModifier(
            (TileMultiblockMachineController) (Object) this);
        mmceComplement$maxBatchTime = 0;
    }

    @Inject(method = "hasMachineUpgrade", at = @At("RETURN"), cancellable = true)
    private void mmceComplement$moduleCountsAsUpgrade(String upgradeName,
                                                       CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && mmceComplement$moduleUpgrades.containsKey(upgradeName)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getMachineUpgrade", at = @At("RETURN"), cancellable = true)
    private void mmceComplement$returnModuleUpgrade(String upgradeName,
                                                     CallbackInfoReturnable<MachineUpgrade[]> cir) {
        MachineUpgrade moduleUpgrade = mmceComplement$moduleUpgrades.get(upgradeName);
        if (moduleUpgrade == null) {
            return;
        }
        MachineUpgrade[] original = cir.getReturnValue();
        for (MachineUpgrade upgrade : original) {
            if (upgrade == moduleUpgrade) {
                return;
            }
        }
        MachineUpgrade[] combined = new MachineUpgrade[original.length + 1];
        System.arraycopy(original, 0, combined, 0, original.length);
        combined[original.length] = moduleUpgrade;
        cir.setReturnValue(combined);
    }

    @Inject(method = "writeCustomNBT", at = @At("RETURN"))
    private void mmceComplement$writeAttachmentModules(NBTTagCompound compound, CallbackInfo ci) {
        NBTTagList list = new NBTTagList();
        for (String id : mmceComplement$activeModules) {
            list.appendTag(new NBTTagString(id));
        }
        // Always write the tag, including an empty list. Tile update packets are
        // snapshots, so omitting it would leave an already loaded client with a
        // stale module set after the last active module becomes invalid.
        compound.setTag(mmceComplement$NBT_MODULES, list);
    }

    @Inject(method = "readCustomNBT", at = @At("RETURN"))
    private void mmceComplement$readAttachmentModules(NBTTagCompound compound, CallbackInfo ci) {
        LinkedHashSet<String> saved = new LinkedHashSet<>();
        NBTTagList list = compound.getTagList(mmceComplement$NBT_MODULES, 8);
        for (int i = 0; i < list.tagCount(); i++) {
            String id = list.getStringTagAt(i).trim();
            // The server is authoritative for active modules. In particular,
            // do not filter a client update through its current foundMachine:
            // during chunk/GUI synchronization that definition may not have
            // been restored yet, which used to permanently discard valid IDs.
            if (!id.isEmpty() && !AttachmentResolver.MAIN.equals(id)) {
                saved.add(id);
            }
        }
        mmceComplement$replaceActiveModules(saved, true);
    }

    @Unique
    private void mmceComplement$refreshModules(TileMultiblockMachineController controller) {
        Map<String, AttachmentModule> definitions = mmceComplement$getDefinitions();
        if (foundMachine == null || controllerRotation == null || definitions.isEmpty()) {
            mmceComplement$replaceActiveModules(Collections.emptySet(), false);
            return;
        }

        LinkedHashSet<String> matched = new LinkedHashSet<>();
        Map<String, Collection<String>> dependencies = new LinkedHashMap<>();
        Map<String, Collection<String>> conflicts = new LinkedHashMap<>();
        for (AttachmentModule module : definitions.values()) {
            dependencies.put(module.getId(), module.getDependencies());
            conflicts.put(module.getId(), module.getConflicts());
            TaggedPositionBlockArray effectivePattern = module.getEffectivePattern(
                foundMachine.getPattern(), definitions);
            TaggedPositionBlockArray pattern = AttachmentPatternResolver.getRotatedPattern(
                effectivePattern, controllerRotation);
            if (pattern == null) {
                continue;
            }
            if (!controller.getWorld().isAreaLoaded(pattern.getPatternBoundingBox(controller.getPos()))) {
                if (mmceComplement$activeModules.contains(module.getId())) {
                    matched.add(module.getId());
                }
                continue;
            }
            boolean oldState = mmceComplement$activeModules.contains(module.getId());
            // Attachment parts are parsed with the same MMCE structure format as
            // the main machine. They must therefore use the main machine's
            // matching replacements as well (controller replacements,
            // multiblock modifiers, and other position-specific alternatives).
            // Passing null here made a correctly built module fail as soon as
            // one of its entries needed such a replacement.
            if (pattern.matches(controller.getWorld(), controller.getPos(), oldState,
                foundReplacements)) {
                matched.add(module.getId());
            }
        }
        mmceComplement$replaceActiveModules(
            AttachmentResolver.resolve(matched, dependencies, conflicts), false);
    }

    @Unique
    private Map<String, AttachmentModule> mmceComplement$getDefinitions() {
        if (foundMachine == null) {
            return Collections.emptyMap();
        }
        return ((AttachmentMachine) (Object) foundMachine).mmceComplement$getAttachmentModules();
    }

    @Unique
    private void mmceComplement$combineActivePatterns() {
        if (mmceComplement$mainPattern == null || mmceComplement$activeModules.isEmpty()) {
            mmceComplement$combinedPattern = null;
            foundPattern = mmceComplement$mainPattern;
            return;
        }
        Map<String, AttachmentModule> definitions = mmceComplement$getDefinitions();
        TaggedPositionBlockArray combined = new TaggedPositionBlockArray(mmceComplement$mainPattern);
        for (String id : mmceComplement$activeModules) {
            AttachmentModule module = definitions.get(id);
            if (module == null) {
                continue;
            }
            TaggedPositionBlockArray effectivePattern = module.getEffectivePattern(
                foundMachine.getPattern(), definitions);
            TaggedPositionBlockArray rotated = AttachmentPatternResolver.getRotatedPattern(
                effectivePattern, controllerRotation);
            if (rotated != null) {
                AttachmentPatternResolver.appendPreservingParent(combined, rotated);
            }
        }
        combined.flushTileBlocksCache();
        mmceComplement$combinedPattern = combined;
        foundPattern = combined;
    }

    @Unique
    private void mmceComplement$replaceActiveModules(Set<String> newModules, boolean rebuildUpgrades) {
        boolean changed = !mmceComplement$activeModules.equals(newModules);
        mmceComplement$activeModules.clear();
        mmceComplement$activeModules.addAll(newModules);
        if (rebuildUpgrades) {
            mmceComplement$rebuildSyntheticUpgrades();
        }

        if (changed) {
            TileMultiblockMachineController controller = (TileMultiblockMachineController) (Object) this;
            controller.setSearchRecipeImmediately(true);
            if (controller.getWorld() != null && !controller.getWorld().isRemote) {
                controller.markForUpdateSync();
            }
        }
    }

    @Unique
    private void mmceComplement$rebuildSyntheticUpgrades() {
        mmceComplement$removeSyntheticUpgrades();
        Map<String, AttachmentModule> definitions = mmceComplement$getDefinitions();
        for (String id : mmceComplement$activeModules) {
            AttachmentModule module = definitions.get(id);
            if (module == null || !module.isUpgrade()) {
                continue;
            }
            SimpleMachineUpgrade upgrade = new SimpleMachineUpgrade(new UpgradeType(id, id, 1.0F, 1));
            mmceComplement$moduleUpgrades.put(id, upgrade);
            foundUpgrades.computeIfAbsent(id, ignored -> new ArrayList<>()).add(upgrade);
        }
    }

    @Unique
    private void mmceComplement$removeSyntheticUpgrades() {
        for (Map.Entry<String, MachineUpgrade> entry : mmceComplement$moduleUpgrades.entrySet()) {
            List<MachineUpgrade> upgrades = foundUpgrades.get(entry.getKey());
            if (upgrades == null) {
                continue;
            }
            upgrades.remove(entry.getValue());
            if (upgrades.isEmpty()) {
                foundUpgrades.remove(entry.getKey(), upgrades);
            }
        }
        mmceComplement$moduleUpgrades.clear();
    }

    @Unique
    private void mmceComplement$refreshOverclockHatches(
        TileMultiblockMachineController controller) {
        World world = controller.getWorld();
        if (foundPattern == null || world == null) {
            mmceComplement$clearOverclockModifiers(controller);
            return;
        }

        int[] counts = new int[BlockOverclockHatch.OverclockHatchType.values().length];
        BlockPos controllerPos = controller.getPos();
        for (BlockPos relativePos : foundPattern.getPattern().keySet()) {
            IBlockState state = world.getBlockState(controllerPos.add(relativePos));
            if (state.getBlock() == ModBlocks.OVERCLOCK_HATCH) {
                counts[BlockOverclockHatch.getTier(state) - 1]++;
            }
        }

        OverclockHatchLogic.Result result = OverclockHatchLogic.getEffectiveMultipliers(
            counts,
            ModConfig.overclockHatch.getEnergyMultipliers(),
            ModConfig.overclockHatch.getDurationMultipliers(),
            ModConfig.overclockHatch.allowStacking);
        mmceComplement$syncPerformanceModifier(controller,
            mmceComplement$OVERCLOCK_ENERGY_MODIFIER,
            RequirementTypesMM.REQUIREMENT_ENERGY,
            IOType.INPUT,
            result.getEnergyMultiplier());
        mmceComplement$syncPerformanceModifier(controller,
            mmceComplement$OVERCLOCK_DURATION_MODIFIER,
            RequirementTypesMM.REQUIREMENT_DURATION,
            IOType.INPUT,
            result.getDurationMultiplier());
    }

    @Unique
    private void mmceComplement$syncPerformanceModifier(
        TileMultiblockMachineController controller,
        String key,
        hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType<?, ?> type,
        IOType ioType,
        double multiplier) {
        float expected = (float) Math.min(Math.max(0.0D, multiplier), Float.MAX_VALUE);
        RecipeModifier current = controller.getCustomModifiers().get(key);
        boolean neutral = Float.compare(expected, 1.0F) == 0;
        if ((neutral && current == null)
            || (!neutral && current != null
                && current.getTarget() == type
                && current.getIOTarget() == ioType
                && current.getOperation() == RecipeModifier.OPERATION_MULTIPLY
                && !current.affectsChance()
                && Float.compare(current.getModifier(), expected) == 0)) {
            return;
        }
        if (current != null) {
            controller.removePermanentModifier(key);
        }
        if (!neutral) {
            controller.addPermanentModifier(key,
                new RecipeModifier(type, ioType, expected,
                    RecipeModifier.OPERATION_MULTIPLY, false));
        }
    }

    @Unique
    private void mmceComplement$refreshAcceleratorHatches(
        TileMultiblockMachineController controller) {
        World world = controller.getWorld();
        if (foundPattern == null || world == null) {
            mmceComplement$clearAcceleratorModifier(controller);
            return;
        }

        int[] counts = new int[BlockAcceleratorHatch.AcceleratorHatchType.values().length];
        BlockPos controllerPos = controller.getPos();
        for (BlockPos relativePos : foundPattern.getPattern().keySet()) {
            IBlockState state = world.getBlockState(controllerPos.add(relativePos));
            if (state.getBlock() == ModBlocks.ACCELERATOR_HATCH) {
                counts[BlockAcceleratorHatch.getTier(state) - 1]++;
            }
        }

        mmceComplement$syncPerformanceModifier(controller,
            mmceComplement$ACCELERATOR_DURATION_MODIFIER,
            RequirementTypesMM.REQUIREMENT_DURATION,
            IOType.INPUT,
            AcceleratorHatchLogic.getEffectiveDurationMultiplier(counts));
    }

    @Unique
    private void mmceComplement$clearOverclockModifiers(
        TileMultiblockMachineController controller) {
        if (controller.getCustomModifiers().containsKey(
            mmceComplement$OVERCLOCK_ENERGY_MODIFIER)) {
            controller.removePermanentModifier(mmceComplement$OVERCLOCK_ENERGY_MODIFIER);
        }
        if (controller.getCustomModifiers().containsKey(
            mmceComplement$OVERCLOCK_DURATION_MODIFIER)) {
            controller.removePermanentModifier(mmceComplement$OVERCLOCK_DURATION_MODIFIER);
        }
    }

    @Unique
    private void mmceComplement$clearAcceleratorModifier(
        TileMultiblockMachineController controller) {
        if (controller.getCustomModifiers().containsKey(
            mmceComplement$ACCELERATOR_DURATION_MODIFIER)) {
            controller.removePermanentModifier(mmceComplement$ACCELERATOR_DURATION_MODIFIER);
        }
    }

    @Unique
    private void mmceComplement$refreshBatchHatches(
        TileMultiblockMachineController controller) {
        World world = controller.getWorld();
        if (foundPattern == null || world == null) {
            mmceComplement$maxBatchTime = 0;
            return;
        }

        int maxTime = 0;
        BlockPos controllerPos = controller.getPos();
        for (BlockPos relativePos : foundPattern.getPattern().keySet()) {
            net.minecraft.tileentity.TileEntity tile =
                world.getTileEntity(controllerPos.add(relativePos));
            if (tile instanceof TileBatchHatch) {
                maxTime = Math.max(maxTime, ((TileBatchHatch) tile).getMaxBatchTime());
            }
        }
        mmceComplement$maxBatchTime = maxTime;
    }
}

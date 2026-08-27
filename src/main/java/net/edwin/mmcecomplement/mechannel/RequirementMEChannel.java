package net.edwin.mmcecomplement.mechannel;

import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftCheck;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.ResultChance;
import net.edwin.mmcecomplement.compat.jei.mechannel.JEIComponentMEChannel;
import net.edwin.mmcecomplement.compat.jei.mechannel.MEChannelIngredient;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reserves a fixed number of real AE channels for the whole recipe duration.
 */
public class RequirementMEChannel extends ComponentRequirement.PerTickMultiComponent<
    MEChannelIngredient, RequirementTypeMEChannel> {

    private static final String FAILURE_UNAVAILABLE =
        "craftcheck.failure.mmce_complement.me_channel.network_unavailable";
    private static final String FAILURE_BOOTING =
        "craftcheck.failure.mmce_complement.me_channel.network_booting";
    private static final String FAILURE_CONFLICT =
        "craftcheck.failure.mmce_complement.me_channel.controller_conflict";
    private static final String FAILURE_MISMATCH =
        "craftcheck.failure.mmce_complement.me_channel.network_mismatch";
    private static final String FAILURE_INSUFFICIENT =
        "craftcheck.failure.mmce_complement.me_channel.insufficient";
    private static final String FAILURE_RESERVATION =
        "craftcheck.failure.mmce_complement.me_channel.reservation_lost";

    private final int amount;
    private final Map<MEChannelProvider, Integer> activeAssignments =
        new IdentityHashMap<>();
    private int activeAmount;

    public RequirementMEChannel(int amount) {
        super(ModMEChannelTypes.REQUIREMENT, IOType.INPUT);
        if (amount <= 0) {
            throw new IllegalArgumentException("ME channel amount must be positive");
        }
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public int getSortingWeight() {
        return PRIORITY_WEIGHT_ENERGY - 1;
    }

    @Override
    public RequirementMEChannel deepCopy() {
        return (RequirementMEChannel) new RequirementMEChannel(amount)
            .postDeepCopy(this);
    }

    @Override
    public RequirementMEChannel deepCopyModified(List<RecipeModifier> modifiers) {
        long modified = Math.round(RecipeModifier.applyModifiers(
            modifiers, this, (double) amount, false));
        int clamped = (int) Math.max(1L,
            Math.min((long) Integer.MAX_VALUE, modified));
        return (RequirementMEChannel) new RequirementMEChannel(clamped)
            .postDeepCopy(this);
    }

    @Nonnull
    @Override
    public String getMissingComponentErrorMessage(IOType ioType) {
        ResourceLocation key = requirementType.getRegistryName();
        return String.format("component.missing.%s.%s.%s",
            key.getNamespace(), key.getPath(),
            IOType.INPUT.name().toLowerCase());
    }

    @Override
    public JEIComponent<MEChannelIngredient> provideJEIComponent() {
        return new JEIComponentMEChannel(amount);
    }

    @Override
    public boolean isValidComponent(ProcessingComponent<?> component,
                                    RecipeCraftingContext context) {
        MachineComponent<?> machineComponent = component.getComponent();
        return machineComponent.getIOType() == IOType.INPUT
            && machineComponent.getComponentType()
                .equals(ModMEChannelTypes.COMPONENT)
            && component.getProvidedComponent() instanceof MEChannelProvider;
    }

    @Nonnull
    @Override
    public CraftCheck canStartCrafting(List<ProcessingComponent<?>> components,
                                       RecipeCraftingContext context) {
        return checkPlan(createPlan(components, requiredAmount(context)));
    }

    @Override
    public synchronized void startCrafting(
        List<ProcessingComponent<?>> components,
        RecipeCraftingContext context, ResultChance chance) {
        releaseReservation();
        reserve(components, requiredAmount(context));
    }

    @Override
    public synchronized CraftCheck doIOTick(
        List<ProcessingComponent<?>> components,
        RecipeCraftingContext context, float durationMultiplier) {
        int required = requiredAmount(context);
        if (!assignmentsMatch(components, required)) {
            releaseReservation();
            CraftCheck reserveCheck = reserve(components, required);
            if (!reserveCheck.isSuccess()) {
                return reserveCheck;
            }
        }

        Object network = null;
        for (Map.Entry<MEChannelProvider, Integer> entry
            : activeAssignments.entrySet()) {
            MEChannelProvider provider = entry.getKey();
            Object currentNetwork = provider.getMEChannelNetworkIdentity();
            if (currentNetwork == null) {
                return CraftCheck.failure(FAILURE_UNAVAILABLE);
            }
            if (network == null) {
                network = currentNetwork;
            } else if (network != currentNetwork) {
                return CraftCheck.failure(FAILURE_MISMATCH);
            }
            if (!provider.isMEChannelReservationSatisfied(this,
                entry.getValue())) {
                return CraftCheck.failure(FAILURE_RESERVATION);
            }
        }
        return activeAssignments.isEmpty()
            ? CraftCheck.failure(FAILURE_RESERVATION)
            : CraftCheck.success();
    }

    @Override
    public synchronized void finishCrafting(
        List<ProcessingComponent<?>> components,
        RecipeCraftingContext context, ResultChance chance) {
        releaseReservation();
    }

    @Nonnull
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<ProcessingComponent<?>> copyComponents(
        List<ProcessingComponent<?>> components) {
        List<ProcessingComponent<?>> copies = new ArrayList<>(components.size());
        for (ProcessingComponent<?> component : components) {
            copies.add(new ProcessingComponent(
                component.getComponent(), component.getProvidedComponent(),
                component.getTag()));
        }
        return copies;
    }

    /** Called by the context lifecycle mixin on reset, cancel and destroy. */
    public synchronized void releaseReservation() {
        for (MEChannelProvider provider
            : new ArrayList<>(activeAssignments.keySet())) {
            provider.releaseMEChannels(this);
        }
        activeAssignments.clear();
        activeAmount = 0;
    }

    private CraftCheck reserve(List<ProcessingComponent<?>> components,
                               int required) {
        MEChannelAllocationPlanner.Result plan = createPlan(components, required);
        CraftCheck check = checkPlan(plan);
        if (!check.isSuccess()) {
            return check;
        }

        for (Map.Entry<MEChannelProvider, Integer> entry
            : plan.getAssignments().entrySet()) {
            if (!entry.getKey().requestMEChannels(this, entry.getValue())) {
                releaseReservation();
                return CraftCheck.failure(FAILURE_RESERVATION);
            }
            activeAssignments.put(entry.getKey(), entry.getValue());
        }
        activeAmount = required;
        return CraftCheck.success();
    }

    private boolean assignmentsMatch(List<ProcessingComponent<?>> components,
                                     int required) {
        if (activeAmount != required || activeAssignments.isEmpty()) {
            return false;
        }
        IdentityHashMap<MEChannelProvider, Boolean> available =
            new IdentityHashMap<>();
        for (ProcessingComponent<?> component : components) {
            if (component.getProvidedComponent() instanceof MEChannelProvider) {
                available.put((MEChannelProvider) component.getProvidedComponent(),
                    Boolean.TRUE);
            }
        }
        for (MEChannelProvider provider : activeAssignments.keySet()) {
            if (!available.containsKey(provider)) {
                return false;
            }
        }
        return true;
    }

    private MEChannelAllocationPlanner.Result createPlan(
        List<ProcessingComponent<?>> components, int required) {
        List<MEChannelProvider> providers = new ArrayList<>();
        for (ProcessingComponent<?> component : components) {
            if (component.getProvidedComponent() instanceof MEChannelProvider) {
                providers.add((MEChannelProvider) component.getProvidedComponent());
            }
        }
        return MEChannelAllocationPlanner.plan(providers, required);
    }

    private static CraftCheck checkPlan(MEChannelAllocationPlanner.Result plan) {
        if (plan.isSuccess()) {
            return CraftCheck.success();
        }
        switch (plan.getFailure()) {
            case NETWORK_BOOTING:
                return CraftCheck.failure(FAILURE_BOOTING);
            case CONTROLLER_CONFLICT:
                return CraftCheck.failure(FAILURE_CONFLICT);
            case NETWORK_MISMATCH:
                return CraftCheck.failure(FAILURE_MISMATCH);
            case INSUFFICIENT_CHANNELS:
                return CraftCheck.failure(FAILURE_INSUFFICIENT);
            case NETWORK_UNAVAILABLE:
            default:
                return CraftCheck.failure(FAILURE_UNAVAILABLE);
        }
    }

    private int requiredAmount(RecipeCraftingContext context) {
        double modified = RecipeModifier.applyModifiers(
            context, this, (double) amount, false);
        if (Double.isNaN(modified) || modified <= 1D) {
            return 1;
        }
        if (modified >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, (int) Math.round(modified));
    }
}

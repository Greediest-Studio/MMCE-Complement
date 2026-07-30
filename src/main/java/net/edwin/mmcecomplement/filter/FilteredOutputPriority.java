package net.edwin.mmcecomplement.filter;

import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.util.HybridFluidUtils;
import hellfirepvp.modularmachinery.common.util.ItemUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Priority and snapshot helpers shared by the item/fluid requirement mixins. */
public final class FilteredOutputPriority {

    private FilteredOutputPriority() { }

    public static List<ProcessingComponent<?>> prioritize(
        List<ProcessingComponent<?>> components) {
        if (components == null || components.size() < 2
            || !containsFiltered(components)) return components;
        List<ProcessingComponent<?>> result = new ArrayList<>(components);
        result.sort(Comparator.comparingInt(component ->
            component.getComponent() instanceof FilteredOutputComponent ? 0 : 1));
        return result;
    }

    public static boolean containsFiltered(
        List<ProcessingComponent<?>> components) {
        if (components == null) return false;
        for (ProcessingComponent<?> component : components) {
            if (component.getComponent() instanceof FilteredOutputComponent) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List<ProcessingComponent<?>> copyItems(
        List<ProcessingComponent<?>> components) {
        List<ProcessingComponent<?>> result = new ArrayList<>();
        for (ProcessingComponent<?> component : components) {
            Object handler = component.getProvidedComponent();
            if (component.getComponent() instanceof FilteredOutputComponent
                && handler instanceof FilteredItemRecipeHandler) {
                result.add(new ProcessingComponent(component.getComponent(),
                    ((FilteredItemRecipeHandler) handler).copyForSimulation(),
                    component.getTag()));
            } else {
                result.addAll(ItemUtils.copyItemHandlerComponents(
                    Collections.singletonList(component)));
            }
        }
        return prioritize(result);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List<ProcessingComponent<?>> copyFluids(
        List<ProcessingComponent<?>> components) {
        List<ProcessingComponent<?>> result = new ArrayList<>();
        for (ProcessingComponent<?> component : components) {
            Object handler = component.getProvidedComponent();
            if (component.getComponent() instanceof FilteredOutputComponent
                && handler instanceof FilteredFluidRecipeHandler) {
                result.add(new ProcessingComponent(component.getComponent(),
                    ((FilteredFluidRecipeHandler) handler).copyForSimulation(),
                    component.getTag()));
            } else {
                result.addAll(HybridFluidUtils.copyFluidHandlerComponents(
                    Collections.singletonList(component)));
            }
        }
        return prioritize(result);
    }
}

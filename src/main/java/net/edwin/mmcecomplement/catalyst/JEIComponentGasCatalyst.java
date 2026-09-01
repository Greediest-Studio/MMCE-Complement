package net.edwin.mmcecomplement.catalyst;

import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.jei.JEIComponentItem;
import hellfirepvp.modularmachinery.common.integration.recipe.RecipeLayoutPart;
import mekanism.api.gas.GasStack;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import java.awt.Point;
import java.util.Collections;
import java.util.List;

public class JEIComponentGasCatalyst extends ComponentRequirement.JEIComponent<GasStack> {
    private final RequirementGasCatalyst requirement;
    public JEIComponentGasCatalyst(RequirementGasCatalyst requirement) { this.requirement = requirement; }
    @Override public Class<GasStack> getJEIRequirementClass() { return GasStack.class; }
    @Override public List<GasStack> getJEIIORequirements() { return Collections.singletonList(requirement.required); }
    @Override @SideOnly(Side.CLIENT) public RecipeLayoutPart<GasStack> getLayoutPart(Point offset) { return new RecipeLayoutPart.GasTank(offset); }
    @Override @SideOnly(Side.CLIENT) public void onJEIHoverTooltip(int slot, boolean input, GasStack ingredient, List<String> tooltip) {
        JEIComponentItem.addChanceTooltip(input, tooltip, requirement.chance); tooltip.add(I18n.format("tooltip.machinery.catalyst"));
        if (!requirement.getToolTipList().isEmpty()) { tooltip.add(I18n.format("tooltip.machinery.catalyst.effect")); tooltip.addAll(requirement.getToolTipList()); }
    }
}

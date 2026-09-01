package net.edwin.mmcecomplement.catalyst;

import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.jei.JEIComponentItem;
import hellfirepvp.modularmachinery.common.integration.recipe.RecipeLayoutPart;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import java.awt.Point;
import java.util.Collections;
import java.util.List;

public class JEIComponentFluidCatalyst extends ComponentRequirement.JEIComponent<FluidStack> {
    private final RequirementFluidCatalyst requirement;
    public JEIComponentFluidCatalyst(RequirementFluidCatalyst requirement) { this.requirement = requirement; }
    @Override public Class<FluidStack> getJEIRequirementClass() { return FluidStack.class; }
    @Override public List<FluidStack> getJEIIORequirements() { return Collections.singletonList(requirement.required); }
    @Override @SideOnly(Side.CLIENT) public RecipeLayoutPart<FluidStack> getLayoutPart(Point offset) { return new RecipeLayoutPart.FluidTank(offset); }
    @Override @SideOnly(Side.CLIENT) public void onJEIHoverTooltip(int slot, boolean input, FluidStack ingredient, List<String> tooltip) {
        JEIComponentItem.addChanceTooltip(input, tooltip, requirement.chance);
        tooltip.add(I18n.format("tooltip.machinery.catalyst"));
        if (!requirement.getToolTipList().isEmpty()) { tooltip.add(I18n.format("tooltip.machinery.catalyst.effect")); tooltip.addAll(requirement.getToolTipList()); }
    }
}

package net.edwin.mmcecomplement.item;

import net.edwin.mmcecomplement.compat.mmcea.tile.TileConfigurableBiomeProvider;
import net.edwin.mmcecomplement.compat.mmcea.tile.TileConfigurableDimensionProvider;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/** Stores the current dimension and biome, then applies them to configurable detector hatches. */
public class ItemMechanicalBindingTool extends Item {

    private static final String TAG_BINDING = "MMCEBinding";
    private static final String TAG_DIMENSION = "Dimension";
    private static final String TAG_BIOME = "Biome";

    public ItemMechanicalBindingTool() {
        setMaxStackSize(1);
        setCreativeTab(hellfirepvp.modularmachinery.common.CommonProxy.creativeTabModularMachinery);
    }

    @Override
    @Nonnull
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking()) {
            if (!world.isRemote) {
                captureCurrentLocation(stack, world, player);
                player.sendStatusMessage(new TextComponentTranslation(
                        "item.mmce_complement.mechanical_binding_tool.bound"), true);
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        return new ActionResult<>(EnumActionResult.PASS, stack);
    }

    @Override
    @Nonnull
    public EnumActionResult onItemUse(@Nonnull EntityPlayer player, @Nonnull World world,
                                      @Nonnull BlockPos pos, @Nonnull EnumHand hand,
                                      @Nonnull EnumFacing facing, float hitX, float hitY,
                                      float hitZ) {
        if (player.isSneaking()) {
            if (!world.isRemote) {
                captureCurrentLocation(player.getHeldItem(hand), world, player);
            }
            return EnumActionResult.SUCCESS;
        }
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }

        TileEntity tileEntity = world.getTileEntity(pos);
        boolean biomeTarget = tileEntity instanceof TileConfigurableBiomeProvider;
        boolean dimensionTarget = tileEntity instanceof TileConfigurableDimensionProvider;
        if (!biomeTarget && !dimensionTarget) {
            return EnumActionResult.PASS;
        }

        NBTTagCompound binding = getBinding(player.getHeldItem(hand));
        if (binding == null) {
            player.sendStatusMessage(new TextComponentTranslation(
                    "item.mmce_complement.mechanical_binding_tool.empty"), true);
            return EnumActionResult.FAIL;
        }

        if (biomeTarget) {
            TileConfigurableBiomeProvider tile = (TileConfigurableBiomeProvider) tileEntity;
            if (binding.hasKey(TAG_BIOME)) {
                tile.bindBiome(binding.getString(TAG_BIOME));
                player.sendStatusMessage(new TextComponentTranslation(
                        "item.mmce_complement.mechanical_binding_tool.applied"), true);
                return EnumActionResult.SUCCESS;
            }
        } else {
            TileConfigurableDimensionProvider tile = (TileConfigurableDimensionProvider) tileEntity;
            if (binding.hasKey(TAG_DIMENSION)) {
                tile.bindDimension(binding.getInteger(TAG_DIMENSION));
                player.sendStatusMessage(new TextComponentTranslation(
                        "item.mmce_complement.mechanical_binding_tool.applied"), true);
                return EnumActionResult.SUCCESS;
            }
        }
        return EnumActionResult.PASS;
    }

    private static void captureCurrentLocation(ItemStack stack, World world, EntityPlayer player) {
        NBTTagCompound binding = stack.getOrCreateSubCompound(TAG_BINDING);
        binding.setInteger(TAG_DIMENSION, world.provider.getDimension());
        if (world.getBiome(player.getPosition()).getRegistryName() != null) {
            binding.setString(TAG_BIOME, world.getBiome(player.getPosition()).getRegistryName().toString());
        } else {
            binding.removeTag(TAG_BIOME);
        }
    }

    @Nullable
    private static NBTTagCompound getBinding(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().hasKey(TAG_BINDING)
                ? stack.getTagCompound().getCompoundTag(TAG_BINDING) : null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(@Nonnull ItemStack stack, @Nullable World world,
                               @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flag) {
        tooltip.add(I18n.format("item.mmce_complement.mechanical_binding_tool.tooltip"));
        NBTTagCompound binding = getBinding(stack);
        if (binding == null) {
            tooltip.add(I18n.format("item.mmce_complement.mechanical_binding_tool.unbound"));
            return;
        }
        if (binding.hasKey(TAG_DIMENSION)) {
            tooltip.add(I18n.format("item.mmce_complement.mechanical_binding_tool.dimension",
                    binding.getInteger(TAG_DIMENSION)));
        }
        if (binding.hasKey(TAG_BIOME)) {
            tooltip.add(I18n.format("item.mmce_complement.mechanical_binding_tool.biome",
                    binding.getString(TAG_BIOME)));
        }
    }
}

package net.edwin.mmcecomplement.compat.ae.block;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.util.AEPartLocation;
import appeng.core.sync.GuiBridge;
import appeng.items.tools.quartz.ToolQuartzCuttingKnife;
import appeng.util.Platform;
import github.kasuminova.mmce.common.block.appeng.BlockMEPatternProvider;
import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEDataPatternProvider;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemBlock;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Level-one ME pattern provider with a smart-data interface.
 *
 * <p>The original MMCE provider hard-codes its own dropped item and derives
 * the memory-card protocol name from its translation key. This subclass must
 * therefore own the complete interaction and NBT-preserving drop path rather
 * than delegating those operations to the parent block.</p>
 */
public class BlockMEDataPatternProvider extends BlockMEPatternProvider {

    /** MMCE's pattern mirror validates this exact memory-card settings name. */
    public static final String MEMORY_CARD_PROVIDER_TYPE =
        "tile.modularmachinery.blockmepatternprovider";
    /** Whimcraft binds compatible providers through its link card item. */
    public static final ResourceLocation WHIMCRAFT_LINK_CARD =
        new ResourceLocation("whimcraft", "link_card");
    public static final String GROUP_INPUT_TOOLTIP = "tooltip.groupinput.block";
    public static final String DATA_CONFIG_TOOLTIP =
        "tile.mmce_complement.me_data_pattern_provider.tip.configure";

    public BlockMEDataPatternProvider() {
        ((Block) this).setTranslationKey(
            "mmce_complement.me_data_pattern_provider");
    }

    @Override
    public boolean hasTileEntity(@Nonnull IBlockState state) {
        return true;
    }

    @Override
    public boolean onBlockActivated(@Nonnull World world,
                                    @Nonnull BlockPos pos,
                                    @Nonnull IBlockState state,
                                    @Nonnull EntityPlayer player,
                                    @Nonnull EnumHand hand,
                                    @Nonnull EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (!held.isEmpty() && held.getItem() instanceof ItemBlock) {
            return false;
        }
        if (world.isRemote) {
            // Let Whimcraft's Item#onItemUse receive sneak-use for binding.
            if (hand == EnumHand.MAIN_HAND && player.isSneaking()
                && !held.isEmpty() && WHIMCRAFT_LINK_CARD.equals(
                    held.getItem().getRegistryName())) {
                return false;
            }
            return world.getTileEntity(pos)
                instanceof TileMEDataPatternProvider;
        }

        if (hand == EnumHand.MAIN_HAND && !held.isEmpty()) {
            if (player.isSneaking() && WHIMCRAFT_LINK_CARD.equals(
                held.getItem().getRegistryName())) {
                return false;
            }
            if (player.isSneaking()
                && held.getItem() instanceof IMemoryCard) {
                NBTTagCompound data = new NBTTagCompound();
                data.setLong("Pos", pos.toLong());
                ((IMemoryCard) held.getItem()).setMemoryCardContents(held,
                    MEMORY_CARD_PROVIDER_TYPE, data);
                player.sendMessage(new TextComponentTranslation(
                    "message.blockmepatternprovider.save"));
                return true;
            }
            if (held.getItem() instanceof ToolQuartzCuttingKnife) {
                if (ForgeEventFactory.onItemUseStart(player, held, 1) <= 0) {
                    return false;
                }
                TileEntity tile = world.getTileEntity(pos);
                if (tile instanceof TileMEDataPatternProvider) {
                    Platform.openGUI(player, tile,
                        AEPartLocation.fromFacing(facing),
                        GuiBridge.GUI_RENAMER);
                    return true;
                }
                return false;
            }
        }

        if (world.getTileEntity(pos) instanceof TileMEDataPatternProvider) {
            player.openGui(MMCEComplement.instance,
                MMCEComplement.GUI_ME_DATA_PATTERN_PROVIDER, world,
                pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
        return new TileMEDataPatternProvider();
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(@Nonnull World world,
                                       @Nonnull IBlockState state) {
        return new TileMEDataPatternProvider();
    }

    @Override
    public void dropBlockAsItemWithChance(@Nonnull World world,
                                          @Nonnull BlockPos pos,
                                          @Nonnull IBlockState state,
                                          float chance, int fortune) {
        // breakBlock writes the provider and per-pattern data into one item.
    }

    @Override
    public void breakBlock(World world, @Nonnull BlockPos pos,
                           @Nonnull IBlockState state) {
        ItemStack dropped = new ItemStack(this);
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileMEDataPatternProvider
            && !((TileMEDataPatternProvider) tile).isAllDefault()) {
            dropped.setTagInfo("patternProvider",
                ((TileMEDataPatternProvider) tile)
                    .writeProviderNBT(new NBTTagCompound()));
        }
        spawnAsEntity(world, pos, dropped);
        world.removeTileEntity(pos);
    }

    @Override
    public void onBlockPlacedBy(@Nonnull World world, @Nonnull BlockPos pos,
                                @Nonnull IBlockState state,
                                @Nonnull EntityLivingBase placer,
                                @Nonnull ItemStack stack) {
        // BlockMEPatternProvider calls readProviderNBT virtually, so the data
        // type and per-slot marker values are restored by this single call.
        super.onBlockPlacedBy(world, pos, state, placer, stack);
    }

    @Nonnull
    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Nonnull
    @Override
    public EnumBlockRenderType getRenderType(@Nonnull IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean isOpaqueCube(@Nonnull IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(@Nonnull IBlockState state) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(@Nonnull ItemStack stack,
                               @Nullable World world,
                               @Nonnull List<String> tooltip,
                               @Nonnull ITooltipFlag flag) {
        tooltip.add(I18n.format(DATA_CONFIG_TOOLTIP));
        tooltip.add(I18n.format(GROUP_INPUT_TOOLTIP));
    }
}

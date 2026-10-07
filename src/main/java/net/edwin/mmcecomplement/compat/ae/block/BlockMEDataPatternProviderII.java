package net.edwin.mmcecomplement.compat.ae.block;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.util.AEPartLocation;
import appeng.core.sync.GuiBridge;
import appeng.items.tools.quartz.ToolQuartzCuttingKnife;
import appeng.util.Platform;
import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEDataPatternProviderII;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEPatternProviderII;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemBlock;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/** Block counterpart of the 144-slot data pattern provider. */
public class BlockMEDataPatternProviderII extends BlockMEPatternProviderII {

    public static final ResourceLocation WHIMCRAFT_LINK_CARD =
        BlockMEPatternProviderII.WHIMCRAFT_LINK_CARD;
    public static final String MEMORY_CARD_PROVIDER_TYPE =
        BlockMEPatternProviderII.MEMORY_CARD_PROVIDER_TYPE;

    public BlockMEDataPatternProviderII() {
        setTranslationKey("mmce_complement.me_data_pattern_provider_ii");
    }

    @Override
    public boolean hasTileEntity(@Nonnull IBlockState state) {
        return true;
    }

    @Override
    public boolean onBlockActivated(@Nonnull World world, @Nonnull BlockPos pos,
                                    @Nonnull IBlockState state,
                                    @Nonnull EntityPlayer player,
                                    @Nonnull EnumHand hand,
                                    @Nonnull EnumFacing facing, float hitX,
                                    float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (!held.isEmpty() && held.getItem() instanceof ItemBlock) {
            return false;
        }
        if (world.isRemote) {
            if (hand == EnumHand.MAIN_HAND && player.isSneaking()
                && !held.isEmpty() && WHIMCRAFT_LINK_CARD.equals(
                    held.getItem().getRegistryName())) {
                return false;
            }
            return true;
        }
        if (hand == EnumHand.MAIN_HAND && !held.isEmpty()) {
            if (player.isSneaking() && WHIMCRAFT_LINK_CARD.equals(
                held.getItem().getRegistryName())) {
                return false;
            }
            if (player.isSneaking() && held.getItem() instanceof IMemoryCard) {
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
                if (!world.isRemote) {
                    tile = upgradeStaleProviderTile(world, pos, tile);
                }
                if (tile instanceof TileMEDataPatternProviderII) {
                    Platform.openGUI(player, tile,
                        AEPartLocation.fromFacing(facing), GuiBridge.GUI_RENAMER);
                    return true;
                }
                return false;
            }
        }
        TileEntity tile = upgradeStaleProviderTile(world, pos,
            world.getTileEntity(pos));
        MMCEComplement.LOGGER.info(
            "Data pattern provider II activated: tile={}, hand={}, held={}",
            tile == null ? "null" : tile.getClass().getName(), hand,
            held.getItem().getRegistryName());
        if (tile instanceof TileMEDataPatternProviderII) {
            player.openGui(MMCEComplement.instance,
                MMCEComplement.GUI_ME_DATA_PATTERN_PROVIDER_II, world,
                pos.getX(), pos.getY(), pos.getZ());
        } else {
            MMCEComplement.LOGGER.error(
                "Cannot open data pattern provider II GUI: unexpected tile {} at {}",
                tile == null ? "null" : tile.getClass().getName(), pos);
        }
        return true;
    }

    @Nullable
    public static TileEntity upgradeStaleProviderTile(World world, BlockPos pos,
                                                      @Nullable TileEntity tile) {
        if (tile == null
            || tile.getClass() != TileMEPatternProviderII.class) {
            return tile;
        }

        NBTTagCompound saved = new NBTTagCompound();
        ((TileMEPatternProviderII) tile).writeCustomNBT(saved);
        TileMEDataPatternProviderII upgraded =
            new TileMEDataPatternProviderII();
        world.removeTileEntity(pos);
        world.setTileEntity(pos, upgraded);
        upgraded.readCustomNBT(saved);
        upgraded.markDirty();
        world.notifyBlockUpdate(pos, world.getBlockState(pos),
            world.getBlockState(pos), 3);
        MMCEComplement.LOGGER.info(
            "Upgraded stale pattern provider tile on {} at {} from {} to {}",
            world.isRemote ? "client" : "server", pos,
            TileMEPatternProviderII.class.getName(),
            TileMEDataPatternProviderII.class.getName());
        return upgraded;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
        return new TileMEDataPatternProviderII();
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(@Nonnull World world,
                                       @Nonnull IBlockState state) {
        return new TileMEDataPatternProviderII();
    }

    @Override
    public void dropBlockAsItemWithChance(@Nonnull World world,
                                          @Nonnull BlockPos pos,
                                          @Nonnull IBlockState state,
                                          float chance, int fortune) {
        // breakBlock writes the expanded provider state into the item.
    }

    @Override
    public void breakBlock(World world, @Nonnull BlockPos pos,
                           @Nonnull IBlockState state) {
        ItemStack dropped = new ItemStack(this);
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileMEDataPatternProviderII
            && !((TileMEDataPatternProviderII) tile).isAllDefault()) {
            dropped.setTagInfo("patternProvider",
                ((TileMEDataPatternProviderII) tile)
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
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        NBTTagCompound tag = stack.getTagCompound();
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileMEDataPatternProviderII && tag != null
            && tag.hasKey("patternProvider", 10)) {
            ((TileMEDataPatternProviderII) tile)
                .readProviderNBT(tag.getCompoundTag("patternProvider"));
        }
    }

    @Override
    public void addInformation(@Nonnull ItemStack stack, @Nullable World world,
                               @Nonnull List<String> tooltip,
                               @Nonnull ITooltipFlag flag) {
        tooltip.add(I18n.format(
            "tile.mmce_complement.me_data_pattern_provider_ii.tip.configure"));
        tooltip.add(I18n.format(GROUP_INPUT_TOOLTIP));
    }
}

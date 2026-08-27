package net.edwin.mmcecomplement.compat.ae.block;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.util.AEPartLocation;
import appeng.core.sync.GuiBridge;
import appeng.items.tools.quartz.ToolQuartzCuttingKnife;
import appeng.util.Platform;
import github.kasuminova.mmce.common.block.appeng.BlockMEPatternProvider;
import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEPatternProviderII;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
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

/** Block counterpart of the 144-pattern provider. */
public class BlockMEPatternProviderII extends BlockMEPatternProvider {

    /** MMCE's mirror hatch validates this exact memory-card settings name. */
    public static final String MEMORY_CARD_PROVIDER_TYPE =
        "tile.modularmachinery.blockmepatternprovider";
    /** Whimcraft's link card binds a provider through Item#onItemUse. */
    public static final ResourceLocation WHIMCRAFT_LINK_CARD =
        new ResourceLocation("whimcraft", "link_card");
    /** Keep the one useful interaction hint from MMCE's original provider. */
    public static final String GROUP_INPUT_TOOLTIP =
        "tooltip.groupinput.block";

    public BlockMEPatternProviderII() {
        ((Block) this).setTranslationKey(
            "mmce_complement.me_pattern_provider_ii");
    }

    @Override
    public boolean onBlockActivated(@Nonnull World world,
                                    @Nonnull BlockPos pos,
                                    @Nonnull IBlockState state,
                                    @Nonnull EntityPlayer player,
                                    @Nonnull EnumHand hand,
                                    @Nonnull EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            ItemStack held = player.getHeldItem(hand);
            // Whimcraft's card needs Item#onItemUse to bind the provider.
            if (hand == EnumHand.MAIN_HAND && player.isSneaking()
                && !held.isEmpty() && WHIMCRAFT_LINK_CARD.equals(
                    held.getItem().getRegistryName())) {
                return false;
            }
            // Returning SUCCESS client-side makes Minecraft play the normal
            // hand swing for interactions which open this provider's GUI.
            return world.getTileEntity(pos) instanceof TileMEPatternProviderII;
        }
        ItemStack held = player.getHeldItem(hand);
        if (hand == EnumHand.MAIN_HAND && !held.isEmpty()) {
            // Do not consume Whimcraft's Shift-use. Forge normally skips block
            // activation for this item, but returning false here also keeps
            // binding functional with interaction-routing coremods installed.
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
                if (tile instanceof TileMEPatternProviderII) {
                    Platform.openGUI(player, tile,
                        AEPartLocation.fromFacing(facing),
                        GuiBridge.GUI_RENAMER);
                    return true;
                }
                return false;
            }
        }
        if (world.getTileEntity(pos) instanceof TileMEPatternProviderII) {
            player.openGui(MMCEComplement.instance,
                MMCEComplement.GUI_ME_PATTERN_PROVIDER_II, world,
                pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
        return new TileMEPatternProviderII();
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(@Nonnull World world,
                                       @Nonnull IBlockState state) {
        return new TileMEPatternProviderII();
    }

    @Override
    public void dropBlockAsItemWithChance(@Nonnull World world,
                                          @Nonnull BlockPos pos,
                                          @Nonnull IBlockState state,
                                          float chance, int fortune) {
        // breakBlock writes the complete provider state into one dropped item.
    }

    @Override
    public void breakBlock(World world, @Nonnull BlockPos pos,
                           @Nonnull IBlockState state) {
        ItemStack dropped = new ItemStack(this);
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileMEPatternProviderII
            && !((TileMEPatternProviderII) tile).isAllDefault()) {
            dropped.setTagInfo("patternProvider",
                ((TileMEPatternProviderII) tile)
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
        if (tile instanceof TileMEPatternProviderII && tag != null
            && tag.hasKey("patternProvider", 10)) {
            ((TileMEPatternProviderII) tile)
                .readProviderNBT(tag.getCompoundTag("patternProvider"));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(@Nonnull ItemStack stack,
                               @Nullable World world,
                               @Nonnull List<String> tooltip,
                               @Nonnull ITooltipFlag flag) {
        // Do not call super: Provider II intentionally keeps only MMCE's
        // empty-hand Shift+right-click group-setting hint.
        tooltip.add(I18n.format(GROUP_INPUT_TOOLTIP));
    }
}

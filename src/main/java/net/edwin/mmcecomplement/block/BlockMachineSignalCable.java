package net.edwin.mmcecomplement.block;

import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.block.BlockMachineComponent;
import net.edwin.mmcecomplement.tile.TileMachineSignalCable;
import net.edwin.mmcecomplement.item.ItemBlockMachineSignalCable;
import net.edwin.mmcecomplement.init.ModItems;
import net.edwin.mmcecomplement.machineconnection.MachineConnectionRegistry;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.NonNullList;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/** Color-aware machine signal cable. */
public class BlockMachineSignalCable extends BlockMachineComponent {
    public static final PropertyBool NORTH = PropertyBool.create("north");
    public static final PropertyBool SOUTH = PropertyBool.create("south");
    public static final PropertyBool WEST = PropertyBool.create("west");
    public static final PropertyBool EAST = PropertyBool.create("east");
    public static final PropertyBool UP = PropertyBool.create("up");
    public static final PropertyBool DOWN = PropertyBool.create("down");

    private static final AxisAlignedBB CORE = new AxisAlignedBB(5D / 16D, 5D / 16D, 5D / 16D,
        11D / 16D, 11D / 16D, 11D / 16D);
    /** The colour encoded by this block's registry entry, or null for the uncoloured cable. */
    private final EnumDyeColor defaultColor;

    public BlockMachineSignalCable() {
        this(null);
    }

    public BlockMachineSignalCable(@Nullable EnumDyeColor defaultColor) {
        super(Material.CIRCUITS);
        this.defaultColor = defaultColor;
        Block block = this;
        block.setHardness(0.4F);
        block.setResistance(2.0F);
        super.setSoundType(SoundType.METAL);
        block.setTranslationKey("mmce_complement.machine_signal_cable");
        block.setCreativeTab(CommonProxy.creativeTabModularMachinery);
        setDefaultState(blockState.getBaseState()
            .withProperty(NORTH, false)
            .withProperty(SOUTH, false)
            .withProperty(WEST, false)
            .withProperty(EAST, false)
            .withProperty(UP, false)
            .withProperty(DOWN, false));
    }

    public EnumDyeColor getDefaultDyeColor() {
        return defaultColor;
    }

    @Override protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, NORTH, SOUTH, WEST, EAST, UP, DOWN);
    }

    /** Connection flags are derived from neighboring blocks and are not persisted as metadata. */
    @Override public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Override @Nonnull public IBlockState getStateFromMeta(int meta) {
        return getDefaultState();
    }

    @Override @Nonnull public IBlockState getActualState(@Nonnull IBlockState state,
                                                           IBlockAccess world,
                                                           BlockPos pos) {
        return state
            .withProperty(NORTH, canConnect(world, pos, EnumFacing.NORTH))
            .withProperty(SOUTH, canConnect(world, pos, EnumFacing.SOUTH))
            .withProperty(WEST, canConnect(world, pos, EnumFacing.WEST))
            .withProperty(EAST, canConnect(world, pos, EnumFacing.EAST))
            .withProperty(UP, canConnect(world, pos, EnumFacing.UP))
            .withProperty(DOWN, canConnect(world, pos, EnumFacing.DOWN));
    }

    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override @Nonnull public AxisAlignedBB getBoundingBox(IBlockState state,
                                                             IBlockAccess world,
                                                             BlockPos pos) {
        return pipeBounds(world, pos);
    }

    @Override @Nonnull public AxisAlignedBB getCollisionBoundingBox(IBlockState state,
                                                                      IBlockAccess world,
                                                                      BlockPos pos) {
        return pipeBounds(world, pos);
    }
    @Override @Nonnull public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }
    @Override @Nonnull public EnumBlockRenderType getRenderType(IBlockState state) { return EnumBlockRenderType.MODEL; }

    @Override public int getColorMultiplier(IBlockState state, @Nullable IBlockAccess world,
                                             @Nullable BlockPos pos, int tintIndex) {
        if (world != null && pos != null) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileMachineSignalCable) {
                int color = ((TileMachineSignalCable) tile).getCableColor();
                if (color >= 0) return color;
            }
        }
        return defaultColor == null ? 0xFFFFFF : defaultColor.getColorValue();
    }

    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                               EntityPlayer player, EnumHand hand,
                                               EnumFacing side, float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        if (stack.isEmpty() || stack.getItem() != Items.DYE) return false;
        if (!world.isRemote) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileMachineSignalCable) {
                EnumDyeColor dye = EnumDyeColor.byDyeDamage(stack.getMetadata());
                ((TileMachineSignalCable) tile).setCableColor(dye.getColorValue());
                if (!player.capabilities.isCreativeMode) stack.shrink(1);
            }
        }
        return true;
    }

    @Override public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        super.onBlockAdded(world, pos, state);
        scheduleRefresh(world);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
                                EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (!world.isRemote) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileMachineSignalCable) {
                int color = -1;
                if (stack.getItem() instanceof ItemBlockMachineSignalCable) {
                    color = ((ItemBlockMachineSignalCable) stack.getItem()).getPlacedColor(stack);
                } else if (stack.hasTagCompound() && stack.getTagCompound().hasKey("signalCableColor", 3)) {
                    color = stack.getTagCompound().getInteger("signalCableColor");
                }
                ((TileMachineSignalCable) tile).setCableColor(color);
            }
        }
    }

    @Override public void breakBlock(World world, BlockPos pos, IBlockState state) {
        super.breakBlock(world, pos, state);
        scheduleRefresh(world);
    }

    /** Drop the registry item matching the placed cable's dye, rather than the
     * last ItemBlock registered for this shared block. */
    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos,
                         IBlockState state, int fortune) {
        if (world != null) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileMachineSignalCable
                && ModItems.MACHINE_SIGNAL_CABLE_ITEMS != null) {
                int color = ((TileMachineSignalCable) tile).getCableColor();
                ItemBlockMachineSignalCable item = ModItems.MACHINE_SIGNAL_CABLE_ITEMS[0];
                for (EnumDyeColor dye : EnumDyeColor.values()) {
                    if (dye.getColorValue() == color
                        && dye.getMetadata() + 1 < ModItems.MACHINE_SIGNAL_CABLE_ITEMS.length) {
                        item = ModItems.MACHINE_SIGNAL_CABLE_ITEMS[dye.getMetadata() + 1];
                        break;
                    }
                }
                if (item != null) drops.add(new ItemStack(item));
                return;
            }
        }
        super.getDrops(drops, world, pos, state, fortune);
    }

    @Override public void neighborChanged(IBlockState state, World world, BlockPos pos,
                                          Block blockIn, BlockPos fromPos) {
        super.neighborChanged(state, world, pos, blockIn, fromPos);
        world.markBlockRangeForRenderUpdate(pos, pos);
        scheduleRefresh(world);
    }

    private static void scheduleRefresh(World world) {
        if (!world.isRemote && world.getMinecraftServer() != null) {
            world.getMinecraftServer().addScheduledTask(() -> MachineConnectionRegistry.refresh(world));
        }
    }

    private static AxisAlignedBB pipeBounds(IBlockAccess world, BlockPos pos) {
        if (world == null || pos == null) {
            return CORE;
        }
        AxisAlignedBB bounds = CORE;
        for (EnumFacing facing : EnumFacing.values()) {
            if (!canConnect(world, pos, facing)) continue;
            AxisAlignedBB arm;
            switch (facing) {
                case DOWN: arm = new AxisAlignedBB(5D / 16D, 0D, 5D / 16D, 11D / 16D, 5D / 16D, 11D / 16D); break;
                case UP: arm = new AxisAlignedBB(5D / 16D, 11D / 16D, 5D / 16D, 11D / 16D, 1D, 11D / 16D); break;
                case NORTH: arm = new AxisAlignedBB(5D / 16D, 5D / 16D, 0D, 11D / 16D, 11D / 16D, 5D / 16D); break;
                case SOUTH: arm = new AxisAlignedBB(5D / 16D, 5D / 16D, 11D / 16D, 11D / 16D, 11D / 16D, 1D); break;
                case WEST: arm = new AxisAlignedBB(0D, 5D / 16D, 5D / 16D, 5D / 16D, 11D / 16D, 11D / 16D); break;
                default: arm = new AxisAlignedBB(11D / 16D, 5D / 16D, 5D / 16D, 1D, 11D / 16D, 11D / 16D); break;
            }
            bounds = bounds.union(arm);
        }
        return bounds;
    }

    private static boolean canConnect(IBlockAccess world, BlockPos cablePos,
                                      EnumFacing facing) {
        if (world == null || cablePos == null) return false;
        BlockPos pos = cablePos.offset(facing);
        Block block = world.getBlockState(pos).getBlock();
        if (block == ModBlocks.MACHINE_SIGNAL_INPUT_HATCH
            || block == ModBlocks.MACHINE_SIGNAL_OUTPUT_HATCH) {
            return true;
        }
        if (!(block instanceof BlockMachineSignalCable)) return false;
        // Use the registry colour as a fallback while a client TileEntity is
        // being created or before its NBT has arrived.  Returning true merely
        // because a TileEntity is absent made every pair of coloured cables
        // appear connected during world updates.
        int a = effectiveCableColor(world, cablePos);
        int b = effectiveCableColor(world, pos);
        return a < 0 || b < 0 || a == b;
    }

    private static int effectiveCableColor(IBlockAccess world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileMachineSignalCable) {
            int color = ((TileMachineSignalCable) tile).getCableColor();
            if (color >= 0) return color;
        }
        Block block = world.getBlockState(pos).getBlock();
        if (block instanceof BlockMachineSignalCable) {
            EnumDyeColor color = ((BlockMachineSignalCable) block).getDefaultDyeColor();
            return color == null ? -1 : color.getColorValue();
        }
        return -1;
    }

    @Override @Nullable public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
        TileMachineSignalCable tile = new TileMachineSignalCable();
        if (defaultColor != null) {
            tile.initializeCableColor(defaultColor.getColorValue());
        }
        return tile;
    }

    @Override @SideOnly(Side.CLIENT) public void addInformation(@Nonnull ItemStack stack,
                                                                  @Nullable World world,
                                                                  @Nonnull List<String> tooltip,
                                                                  @Nonnull ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        tooltip.add(I18n.format("tile.mmce_complement.machine_signal_cable.tip"));
    }
}

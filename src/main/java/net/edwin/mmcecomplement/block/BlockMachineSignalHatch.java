package net.edwin.mmcecomplement.block;

import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.block.BlockMachineComponent;
import net.edwin.mmcecomplement.machineconnection.MachineConnectionRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Shared shell for the machine signal input/output hatches. */
public abstract class BlockMachineSignalHatch extends BlockMachineComponent {
    protected BlockMachineSignalHatch(String key) {
        super(Material.IRON);
        Block block = this;
        block.setHardness(2.0F);
        block.setResistance(10.0F);
        super.setSoundType(SoundType.METAL);
        block.setHarvestLevel("pickaxe", 1);
        block.setTranslationKey(key);
        block.setCreativeTab(CommonProxy.creativeTabModularMachinery);
    }

    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override @Nonnull public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }
    @Override @Nonnull public EnumBlockRenderType getRenderType(IBlockState state) { return EnumBlockRenderType.MODEL; }

    @Override public void onBlockAdded(World world, net.minecraft.util.math.BlockPos pos,
                                       IBlockState state) {
        super.onBlockAdded(world, pos, state);
        scheduleRefresh(world);
    }

    @Override public void breakBlock(World world, net.minecraft.util.math.BlockPos pos,
                                     IBlockState state) {
        super.breakBlock(world, pos, state);
        scheduleRefresh(world);
    }

    private static void scheduleRefresh(World world) {
        if (!world.isRemote && world.getMinecraftServer() != null) {
            world.getMinecraftServer().addScheduledTask(() ->
                MachineConnectionRegistry.refresh(world));
        }
    }

    @Override @Nullable public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) {
        return createNewTileEntity(world, 0);
    }
}

package net.edwin.mmcecomplement.block;

import hellfirepvp.modularmachinery.common.block.BlockFactoryController;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.variant.StorageController;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Storage counterpart for MMCE's optional integrated/factory controller. */
public class BlockStorageFactoryController extends BlockFactoryController {
    public BlockStorageFactoryController(DynamicMachine machine) {
        super(machine);
        if (machine != null && machine.getRegistryName() != null) {
            String path = machine.getRegistryName().getPath()
                + "_storage_controller";
            setRegistryName(machine.getRegistryName().getNamespace(), path);
            setTranslationKey(machine.getRegistryName().getNamespace() + "." + path);
        }
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new net.edwin.mmcecomplement.tile.TileStorageController(this, state);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new net.edwin.mmcecomplement.tile.TileStorageController(this,
            getStateFromMeta(meta));
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY,
                                    float hitZ) {
        if (player.isSneaking()) {
            return false;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof StorageController
            && ((StorageController) tile).mmceComplement$isStorageController()) {
            player.swingArm(hand);
            if (!world.isRemote) {
                player.openGui(MMCEComplement.instance, MMCEComplement.GUI_STORAGE,
                    world, pos.getX(), pos.getY(), pos.getZ());
            }
            return true;
        }
        return super.onBlockActivated(world, pos, state, player, hand, facing,
            hitX, hitY, hitZ);
    }

    @Override
    public String getLocalizedName() {
        DynamicMachine machine = getParentMachine();
        return machine == null ? super.getLocalizedName()
            : machine.getLocalizedName() + "\u5b58\u50a8\u63a7\u5236\u5668";
    }
}

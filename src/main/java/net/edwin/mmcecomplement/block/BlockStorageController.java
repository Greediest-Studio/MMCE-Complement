package net.edwin.mmcecomplement.block;

import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.variant.StorageMachine;
import net.edwin.mmcecomplement.variant.StorageController;
import net.edwin.mmcecomplement.tile.TileStorageController;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;

/** MMCE controller block used by storage variants. */
public class BlockStorageController extends BlockController {
    public BlockStorageController(DynamicMachine machine) {
        // Do not use BlockController(DynamicMachine): that constructor assigns
        // MMCE's regular <machine>_controller registry name.  Storage
        // variants have their own distinct id and Forge forbids changing an
        // entry's registry name after it has been assigned.
        super();
        this.parentMachine = machine;
        configureName(machine);
    }

    private void configureName(DynamicMachine machine) {
        ResourceLocation id = machine == null ? null : machine.getRegistryName();
        if (id == null) return;
        // Keep the machine id first and add the storage marker before the
        // controller suffix: <machineId>_storage_controller.
        String path = id.getPath() + "_storage_controller";
        setRegistryName(new ResourceLocation(id.getNamespace(), path));
        setTranslationKey(id.getNamespace() + "." + path);
    }

    private boolean isStorageMachine() {
        DynamicMachine machine = getParentMachine();
        return machine instanceof StorageMachine
            && ((StorageMachine) (Object) machine).mmceComplement$isStorageVariant();
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return isStorageMachine() ? new TileStorageController(this, state)
            : super.createTileEntity(world, state);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return isStorageMachine() ? new TileStorageController(this,
            getStateFromMeta(meta)) : super.createNewTileEntity(world, meta);
    }

    @Override
    public String getLocalizedName() {
        DynamicMachine machine = getParentMachine();
        return isStorageMachine() && machine != null
            ? machine.getLocalizedName() + "\u5b58\u50a8\u63a7\u5236\u5668"
            : super.getLocalizedName();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY,
                                    float hitZ) {
        // Match vanilla block interaction semantics while sneaking: a
        // sneaking use must remain available for placement/other block
        // interactions and must never open this container.
        if (player.isSneaking()) {
            return false;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof StorageController
            && ((StorageController) tile).mmceComplement$isStorageController()) {
            player.swingArm(hand);
            if (!world.isRemote) {
                int gui = tile instanceof TileStorageController
                    && ((TileStorageController) tile).isStructureFormed()
                    ? MMCEComplement.GUI_STORAGE
                    : MMCEComplement.GUI_STORAGE_UNFORMED;
                player.openGui(MMCEComplement.instance, gui,
                    world, pos.getX(), pos.getY(), pos.getZ());
            }
            return true;
        }
        return super.onBlockActivated(world, pos, state, player, hand, facing,
            hitX, hitY, hitZ);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof StorageController) {
                dropInventory(world, pos,
                    ((StorageController) tile).mmceComplement$getStorageInventory());
            }
        }
        super.breakBlock(world, pos, state);
    }

    /** Drops every stored stack when the controller is harvested. */
    public static void dropInventory(World world, BlockPos pos,
                                     ItemStackHandler inventory) {
        if (inventory == null) return;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack == null || stack.isEmpty()) continue;
            EntityItem entity = new EntityItem(world,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                stack.copy());
            entity.motionX = (world.rand.nextDouble() - 0.5D) * 0.15D;
            entity.motionY = world.rand.nextDouble() * 0.2D + 0.1D;
            entity.motionZ = (world.rand.nextDouble() - 0.5D) * 0.15D;
            world.spawnEntity(entity);
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
    }
}

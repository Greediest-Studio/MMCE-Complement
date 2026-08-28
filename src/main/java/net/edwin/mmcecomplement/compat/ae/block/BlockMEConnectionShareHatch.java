package net.edwin.mmcecomplement.compat.ae.block;

import github.kasuminova.mmce.common.block.appeng.BlockMEMachineComponent;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEConnectionShareHatch;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/** Block shell for an AE network connection sharing hatch. */
public class BlockMEConnectionShareHatch extends BlockMEMachineComponent {
    public BlockMEConnectionShareHatch() {
        setTranslationKey("mmce_complement.me_connection_share_hatch");
    }
    @Override
    public boolean onBlockActivated(@Nonnull World world, @Nonnull BlockPos pos,
        @Nonnull IBlockState state, @Nonnull EntityPlayer player,
        @Nonnull EnumHand hand, @Nonnull EnumFacing facing,
        float hitX, float hitY, float hitZ) { return true; }
    @Nullable
    @Override
    public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
        return new TileMEConnectionShareHatch();
    }
    @Nullable
    @Override
    public TileEntity createTileEntity(@Nonnull World world,
        @Nonnull IBlockState state) { return new TileMEConnectionShareHatch(); }
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(@Nonnull ItemStack stack, @Nullable World world,
        @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        tooltip.add(I18n.format("tile.mmce_complement.me_connection_share_hatch.tip.1"));
        tooltip.add(I18n.format("tile.mmce_complement.me_connection_share_hatch.tip.2"));
    }
}

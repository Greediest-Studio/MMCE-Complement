package net.edwin.mmcecomplement.compat.ae;

import net.edwin.mmcecomplement.compat.ae.tile.TileMEDataPatternProvider;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MEDataPatternProviderPersistenceTest {

    @Test
    void declaresSeparatePlacedTileAndDroppedItemPersistenceContracts()
        throws Exception {
        Class<TileMEDataPatternProvider> tile =
            TileMEDataPatternProvider.class;

        assertEquals(void.class, tile.getDeclaredMethod("readCustomNBT",
            NBTTagCompound.class).getReturnType());
        assertEquals(void.class, tile.getDeclaredMethod("writeCustomNBT",
            NBTTagCompound.class).getReturnType());
        assertEquals(void.class, tile.getDeclaredMethod("readProviderNBT",
            NBTTagCompound.class).getReturnType());
        assertEquals(NBTTagCompound.class, tile.getDeclaredMethod(
            "writeProviderNBT", NBTTagCompound.class).getReturnType());
        assertEquals(List.class,
            tile.getDeclaredMethod("getBoundData").getReturnType());
    }
}

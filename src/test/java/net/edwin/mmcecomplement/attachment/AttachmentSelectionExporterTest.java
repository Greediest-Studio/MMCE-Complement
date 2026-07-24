package net.edwin.mmcecomplement.attachment;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttachmentSelectionExporterTest {

    @Test
    void subtractsEveryPositionInTheControllersCombinedFormedPattern() {
        BlockPos controller = new BlockPos(10, 64, 10);
        BlockPos mainBlock = controller.add(1, 0, 0);
        BlockPos activeModuleBlock = controller.add(2, 0, 0);
        BlockPos newModuleBlock = controller.add(3, 0, 0);
        BlockArray.BlockInformation information = new BlockArray.BlockInformation(
            Collections.emptyList());
        TaggedPositionBlockArray formed = new TaggedPositionBlockArray();
        formed.addBlock(new BlockPos(1, 0, 0), information);
        formed.addBlock(new BlockPos(2, 0, 0), information);

        List<BlockPos> remaining = AttachmentSelectionExporter.subtractFormedPattern(
            Arrays.asList(mainBlock, activeModuleBlock, newModuleBlock), controller, formed);

        assertEquals(Collections.singletonList(newModuleBlock), remaining);
    }

    @Test
    void createsAPasteReadyModuleWithActiveModulesAsParents() {
        LinkedHashSet<String> parents = new LinkedHashSet<>(Arrays.asList("module_1", "module_aux"));

        JsonObject module = new JsonParser().parse(AttachmentSelectionExporter.serializeModule(
            new BlockArray(), "attachment_test", parents)).getAsJsonObject();

        assertEquals("attachment_test", module.get("id").getAsString());
        assertEquals("module_1", module.getAsJsonArray("depends-on").get(0).getAsString());
        assertEquals("module_aux", module.getAsJsonArray("depends-on").get(1).getAsString());
        assertTrue(module.getAsJsonArray("parts").isJsonArray());
    }

    @Test
    void preservesMmceTileNbtWithoutParsingItAsStrictJson() {
        BlockArray output = new BlockArray();
        BlockArray.BlockInformation information = new BlockArray.BlockInformation(
            Collections.emptyList());
        NBTTagCompound matchingTag = new NBTTagCompound();
        matchingTag.setInteger("casingColor", 0x336699);
        information.setMatchingTag(matchingTag);
        output.addBlock(BlockPos.ORIGIN, information);

        String original = output.serializeAsMachineJson();
        String module = AttachmentSelectionExporter.serializeModule(
            output, "attachment_nbt", Collections.singleton("main"));

        assertTrue(module.contains("\"id\": \"attachment_nbt\""));
        assertTrue(module.contains("\"depends-on\""));
        assertTrue(module.contains("\"parts\""));
        assertTrue(module.contains("casingColor"));
        assertTrue(module.contains(original.substring(
            original.indexOf('{') + 1, original.lastIndexOf('}')).trim()));
    }
}

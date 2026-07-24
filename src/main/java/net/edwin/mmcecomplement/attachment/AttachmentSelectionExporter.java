package net.edwin.mmcecomplement.attachment;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import hellfirepvp.modularmachinery.common.network.PktCopyToClipboard;
import hellfirepvp.modularmachinery.common.selection.PlayerStructureSelectionHelper;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.IBlockStateDescriptor;
import net.edwin.mmcecomplement.mixin.AccessorPlayerStructureSelectionHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Exports a shared MMCE selection as a paste-ready attachment module object. */
public final class AttachmentSelectionExporter {

    private static final Gson PRETTY_GSON = new GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create();

    private AttachmentSelectionExporter() {
    }

    public static void finalizeSelection(EnumFacing controllerFacing,
                                         World world,
                                         BlockPos controllerPos,
                                         EntityPlayer player,
                                         TileMultiblockMachineController controller) {
        if (!controller.isStructureFormed()) {
            PlayerStructureSelectionHelper.finalizeSelection(
                controllerFacing, world, controllerPos, player);
            return;
        }

        PlayerStructureSelectionHelper.StructureSelection selection = getSelection(player);
        if (selection == null || selection.getSelectedPositions().isEmpty()) {
            player.sendMessage(new TextComponentTranslation("message.structurebuild.empty"));
            return;
        }

        List<BlockPos> remaining = subtractFormedPattern(selection.getSelectedPositions(),
            controllerPos, controller.getFoundPattern());
        if (remaining.isEmpty()) {
            player.sendMessage(new TextComponentTranslation(
                "message.mmce_complement.attachment_tool.empty_after_subtract"));
            return;
        }

        player.sendMessage(new TextComponentTranslation(
            "message.structurebuild.confirmrotation", controllerFacing.getName()));
        BlockArray output = compressAsArray(remaining, world, controllerPos);
        if (controllerFacing != EnumFacing.NORTH) {
            int rotation = 0;
            EnumFacing face = controllerFacing;
            while (face != EnumFacing.NORTH) {
                output = output.rotateYCCW();
                face = face.rotateYCCW();
                rotation += 90;
            }
            player.sendMessage(new TextComponentTranslation(
                "message.structurebuild.confirmrotation.rotating", String.valueOf(rotation)));
        }

        Set<String> activeParents = ((AttachmentController) controller)
            .mmceComplement$getActiveAttachmentModules();
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String moduleId = "attachment_" + timestamp;
        String serialized = serializeModule(output, moduleId, activeParents);
        saveAndCopy(serialized, moduleId, player);
    }

    public static List<BlockPos> subtractFormedPattern(List<BlockPos> selected,
                                                       BlockPos controllerPos,
                                                       TaggedPositionBlockArray formedPattern) {
        Set<BlockPos> occupied = new LinkedHashSet<>();
        for (BlockPos relativePos : formedPattern.getPattern().keySet()) {
            occupied.add(controllerPos.add((Vec3i) relativePos));
        }
        List<BlockPos> remaining = new ArrayList<>();
        for (BlockPos selectedPos : selected) {
            if (!occupied.contains(selectedPos)) {
                remaining.add(selectedPos);
            }
        }
        return remaining;
    }

    public static String serializeModule(BlockArray output,
                                         String moduleId,
                                         Set<String> activeParents) {
        JsonObject header = new JsonObject();
        header.addProperty("id", moduleId);
        if (!activeParents.isEmpty()) {
            JsonArray dependencies = new JsonArray();
            for (String parent : activeParents) {
                dependencies.add(parent);
            }
            header.add("depends-on", dependencies);
        }

        // MMCE writes tile NBT in its own JSON-like syntax. In particular, values such as
        // casingColor are not necessarily valid strict Gson input. Keep that text byte-for-byte
        // instead of parsing it, and only prepend the attachment fields that we own.
        return prependHeader(output.serializeAsMachineJson(), PRETTY_GSON.toJson(header));
    }

    static String prependHeader(String serializedArray, String serializedHeader) {
        String array = serializedArray.trim();
        String header = serializedHeader.trim();
        int arrayOpen = array.indexOf('{');
        int arrayClose = array.lastIndexOf('}');
        int headerOpen = header.indexOf('{');
        int headerClose = header.lastIndexOf('}');
        if (arrayOpen < 0 || arrayClose <= arrayOpen
            || headerOpen < 0 || headerClose <= headerOpen) {
            throw new IllegalArgumentException("MMCE structure serializer returned an invalid object");
        }

        String arrayMembers = array.substring(arrayOpen + 1, arrayClose).trim();
        String headerMembers = header.substring(headerOpen + 1, headerClose).trim();
        StringBuilder combined = new StringBuilder(array.length() + header.length());
        combined.append("{\n");
        if (!headerMembers.isEmpty()) {
            combined.append(headerMembers);
        }
        if (!headerMembers.isEmpty() && !arrayMembers.isEmpty()) {
            combined.append(",\n");
        }
        if (!arrayMembers.isEmpty()) {
            combined.append(arrayMembers);
        }
        combined.append("\n}");
        return combined.toString();
    }

    private static PlayerStructureSelectionHelper.StructureSelection getSelection(EntityPlayer player) {
        Map<UUID, PlayerStructureSelectionHelper.StructureSelection> selections =
            AccessorPlayerStructureSelectionHelper.mmceComplement$getActiveSelectionMap();
        return selections.get(player.getUniqueID());
    }

    private static BlockArray compressAsArray(List<BlockPos> selected,
                                              World world,
                                              BlockPos center) {
        BlockArray output = new BlockArray();
        for (BlockPos pos : selected) {
            IBlockState state = world.getBlockState(pos);
            IBlockStateDescriptor descriptor = new IBlockStateDescriptor(state);
            BlockArray.BlockInformation information = new BlockArray.BlockInformation(
                Collections.singletonList(descriptor));
            TileEntity tile = world.getTileEntity(pos);
            if (tile != null) {
                NBTTagCompound tag = new NBTTagCompound();
                tile.writeToNBT(tag);
                tag.removeTag("x");
                tag.removeTag("y");
                tag.removeTag("z");
                information.setMatchingTag(tag);
            }
            output.addBlock(pos.subtract((Vec3i) center), information);
        }
        return output;
    }

    private static void saveAndCopy(String serialized,
                                    String moduleId,
                                    EntityPlayer player) {
        if (FMLCommonHandler.instance().getMinecraftServerInstance() == null) {
            return;
        }
        if (player instanceof EntityPlayerMP) {
            ModularMachinery.NET_CHANNEL.sendTo(
                (IMessage) new PktCopyToClipboard(serialized), (EntityPlayerMP) player);
        }

        File directory = CommonProxy.dataHolder.getMachineryDirectory();
        File output = uniqueFile(directory,
            "attachment-module-" + player.getName() + "-" + moduleId);
        try {
            Files.write(serialized, output, StandardCharsets.UTF_8);
            player.sendMessage(new TextComponentTranslation(
                "message.mmce_complement.attachment_tool.save", output.getName(), moduleId));
        } catch (IOException exception) {
            ModularMachinery.log.error("Failed to export attachment module selection", exception);
            player.sendMessage(new TextComponentTranslation("message.structurebuild.fail"));
            if (output.exists() && !output.delete()) {
                ModularMachinery.log.warn("Failed to delete incomplete attachment export {}", output);
            }
        }
    }

    private static File uniqueFile(File directory, String baseName) {
        File output = new File(directory, baseName + ".json");
        int increment = 0;
        while (output.exists()) {
            output = new File(directory, baseName + " (" + increment++ + ").json");
        }
        return output;
    }
}

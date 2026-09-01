package net.edwin.mmcecomplement.preview;

import java.util.List;

/** Stores custom display lines for a Mekanism gas requirement. */
public interface GasTooltipData {
    void mmceComplement$addGasTooltip(String line);

    void mmceComplement$clearGasTooltip();

    List<String> mmceComplement$getGasTooltip();
}

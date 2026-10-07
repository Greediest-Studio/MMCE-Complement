package net.edwin.mmcecomplement.attachment;

import com.google.gson.JsonParseException;

/** Rendering policy for attachment modules when a machine has a custom model. */
public enum AttachmentModelMode {
    /** Keep MMCE's normal component rendering. */
    DEFAULT("none"),
    /** Replace the formed structure with the machine model. */
    HIDE("hide"),
    /** Render the machine and active attachment models independently. */
    SEPARATE("separate");

    private final String serializedName;

    AttachmentModelMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public static AttachmentModelMode parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DEFAULT;
        }
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        for (AttachmentModelMode mode : values()) {
            if (mode.serializedName.equals(normalized)
                || (mode == DEFAULT && "default".equals(normalized))) {
                return mode;
            }
        }
        throw new JsonParseException("'attachment-module-model' must be one of 'none', 'hide', or 'separate'!");
    }
}

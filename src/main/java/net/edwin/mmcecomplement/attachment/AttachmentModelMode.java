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
        // Numeric aliases are accepted for compatibility with early drafts of
        // the machine JSON documentation: 1 hides formed components and 2
        // renders attachment models independently.
        if ("0".equals(normalized)) return DEFAULT;
        if ("1".equals(normalized)) return HIDE;
        if ("2".equals(normalized)) return SEPARATE;
        throw new JsonParseException("'attachment-module-model' must be one of 'none', 'hide', or 'separate'!");
    }
}

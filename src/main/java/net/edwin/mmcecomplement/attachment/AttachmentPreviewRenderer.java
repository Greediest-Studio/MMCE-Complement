package net.edwin.mmcecomplement.attachment;

/** Client-side controls exposed by the MMCE structure preview renderer mixin. */
public interface AttachmentPreviewRenderer {

    String mmceComplement$getPreviewModule();

    void mmceComplement$showNextPreviewModule();

    boolean mmceComplement$previewModuleHasParents();

    boolean mmceComplement$isMergingPreviewParents();

    void mmceComplement$setMergePreviewParents(boolean mergeParents);
}

package net.edwin.mmcecomplement.attachment;

import java.util.Map;

/** Implemented on MMCE dynamic machines by the attachment module mixin. */
public interface AttachmentMachine {

    Map<String, AttachmentModule> mmceComplement$getAttachmentModules();
}

package mockit.external.asm;

import javax.annotation.*;

class BaseWriter
{
   /**
    * The dynamically generated constant pool of the class being built/modified.
    */
   @Nonnull ConstantPoolGeneration cp;

   /**
    * The runtime visible annotations of this class/field/method.
    */
   @Nullable AnnotationWriter annotations;

   @Nonnull
   final AnnotationVisitor addAnnotation(@Nonnull String desc) {
      AnnotationWriter aw = new AnnotationWriter(cp, desc);
      aw.next = annotations;
      annotations = aw;
      return aw;
   }

   @Nonnegative
   final int getAnnotationsSize() {
      if (annotations != null) {
         getConstantPoolItemForRuntimeVisibleAnnotationsAttribute();
         return 8 + annotations.getSize();
      }

      return 0;
   }

   @Nonnegative
   private int getConstantPoolItemForRuntimeVisibleAnnotationsAttribute() {
      return cp.newUTF8("RuntimeVisibleAnnotations");
   }

   final void putAnnotations(@Nonnull ByteVector out) {
      if (annotations != null) {
         int item = getConstantPoolItemForRuntimeVisibleAnnotationsAttribute();
         out.putShort(item);
         annotations.put(out);
      }
   }
}

package mockit.external.asm;

import java.util.*;
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

   /**
    * Visits an annotation of the class/field/method being visited.
    *
    * @param desc the descriptor of the annotation type.
    * @return a visitor to visit the annotation values, or <tt>null</tt> if this visitor is not interested in visiting
    * the annotation.
    */
   @Nullable
   public AnnotationVisitor visitAnnotation(@Nonnull String desc) { return null; }

   /**
    * Visits the end of the class/field/method being visited. This method, which is the last one to be called, is used
    * to inform the visitor that all the annotations and attributes of the class/field/method have been visited.
    */
   public void visitEnd() {}

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

   void put(@Nonnull ByteVector out) {}

   static void put(@Nonnull ByteVector out, @Nonnull List<? extends BaseWriter> writers) {
      out.putShort(writers.size());

      for (BaseWriter writer : writers) {
         writer.put(out);
      }
   }
}

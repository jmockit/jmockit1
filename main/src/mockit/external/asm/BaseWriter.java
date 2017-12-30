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
    * The access flags of this class, field, or method.
    */
   int access;

   @Nonnegative private int deprecatedAttributeIndex;
   @Nonnegative private int syntheticAttributeIndex;

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

   final void createMarkerAttributes(int classVersion) {
      if (Access.isDeprecated(access)) {
         deprecatedAttributeIndex = cp.newUTF8("Deprecated");
      }

      if (Access.isSynthetic(access, classVersion)) {
         syntheticAttributeIndex = cp.newUTF8("Synthetic");
      }
   }

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

   @Nonnegative
   final int getMarkerAttributeCount() {
      return (deprecatedAttributeIndex == 0 ? 0 : 1) + (syntheticAttributeIndex == 0 ? 0 : 1);
   }

   @Nonnegative
   final int getMarkerAttributesSize() {
      int attributeCount = getMarkerAttributeCount();
      return 6 * attributeCount;
   }

   final void putAccess(@Nonnull ByteVector out, int baseMask) {
      int accessFlag = Access.computeFlag(access, baseMask);
      out.putShort(accessFlag);
   }

   final void putMarkerAttributes(@Nonnull ByteVector out) {
      if (deprecatedAttributeIndex > 0) {
         out.putShort(deprecatedAttributeIndex).putInt(0);
      }

      if (syntheticAttributeIndex > 0) {
         out.putShort(syntheticAttributeIndex).putInt(0);
      }
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

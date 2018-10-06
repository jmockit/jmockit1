package mockit.asm;

import java.util.*;
import javax.annotation.*;

import mockit.asm.annotations.*;
import mockit.asm.constantPool.*;
import mockit.asm.jvmConstants.*;
import mockit.asm.util.*;

public class BaseWriter
{
   /**
    * The dynamically generated constant pool of the class being built/modified.
    */
   protected ConstantPoolGeneration cp;

   /**
    * The access flags of this class, field, or method.
    */
   protected int classOrMemberAccess;

   @Nonnegative private int deprecatedAttributeIndex;
   @Nonnegative private int syntheticAttributeIndex;

   /**
    * The runtime visible annotations of this class/field/method.
    */
   @Nullable protected AnnotationVisitor annotations;

   protected BaseWriter() {}

   protected BaseWriter(@Nonnull ConstantPoolGeneration cp, int classOrMemberAccess) {
      this.cp = cp;
      this.classOrMemberAccess = classOrMemberAccess;
   }

   /**
    * Returns the {@link #cp constant pool generation helper object} used by this writer.
    */
   @Nonnull
   public final ConstantPoolGeneration getConstantPoolGeneration() { return cp; }

   /**
    * Visits an annotation of the class/field/method being visited.
    *
    * @param desc the descriptor of the annotation type
    * @return a visitor to visit the annotation values, or <tt>null</tt> if this visitor is not interested in visiting the annotation
    */
   @Nullable
   public AnnotationVisitor visitAnnotation(@Nonnull String desc) {
      return addAnnotation(desc);
   }

   /**
    * Visits the end of the class/field/method being visited. This method, which is the last one to be called, is used to inform the visitor
    * that all the annotations and attributes of the class/field/method have been visited.
    */
   public void visitEnd() {}

   protected final void createMarkerAttributes(int classVersion) {
      if (Access.isDeprecated(classOrMemberAccess)) {
         deprecatedAttributeIndex = cp.newUTF8("Deprecated");
      }

      if (Access.isSynthetic(classOrMemberAccess, classVersion)) {
         syntheticAttributeIndex = cp.newUTF8("Synthetic");
      }
   }

   @Nonnull
   final AnnotationVisitor addAnnotation(@Nonnull String desc) {
      AnnotationVisitor aw = new AnnotationVisitor(cp, desc);
      aw.setNext(annotations);
      annotations = aw;
      return aw;
   }

   @Nonnegative
   protected final int getAnnotationsSize() {
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
   protected final int getMarkerAttributeCount() {
      return (deprecatedAttributeIndex == 0 ? 0 : 1) + (syntheticAttributeIndex == 0 ? 0 : 1);
   }

   @Nonnegative
   protected final int getMarkerAttributesSize() {
      int attributeCount = getMarkerAttributeCount();
      return 6 * attributeCount;
   }

   protected final void putAccess(@Nonnull ByteVector out, int baseMask) {
      int accessFlag = Access.computeFlag(classOrMemberAccess, baseMask);
      out.putShort(accessFlag);
   }

   protected final void putMarkerAttributes(@Nonnull ByteVector out) {
      if (deprecatedAttributeIndex > 0) {
         out.putShort(deprecatedAttributeIndex).putInt(0);
      }

      if (syntheticAttributeIndex > 0) {
         out.putShort(syntheticAttributeIndex).putInt(0);
      }
   }

   protected final void putAnnotations(@Nonnull ByteVector out) {
      if (annotations != null) {
         int item = getConstantPoolItemForRuntimeVisibleAnnotationsAttribute();
         out.putShort(item);
         annotations.put(out);
      }
   }

   protected void put(@Nonnull ByteVector out) {}

   protected static void put(@Nonnull ByteVector out, @Nonnull List<? extends BaseWriter> writers) {
      out.putShort(writers.size());

      for (BaseWriter writer : writers) {
         writer.put(out);
      }
   }
}

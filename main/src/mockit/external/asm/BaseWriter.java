package mockit.external.asm;

class BaseWriter
{
   /**
    * The dynamically generated constant pool of the class being built/modified.
    */
   ConstantPoolGeneration cp;

   /**
    * The runtime visible annotations of this class/field/method. May be <tt>null</tt>.
    */
   AnnotationWriter annotations;

   final AnnotationVisitor addAnnotation(String desc) {
      ByteVector bv = new ByteVector();

      // Write type, and reserve space for values count.
      int type = cp.newUTF8(desc);
      bv.putShort(type).putShort(0);

      AnnotationWriter aw = new AnnotationWriter(cp, true, bv, bv, 2);
      aw.next = annotations;
      annotations = aw;
      return aw;
   }

   final int getAnnotationsSize() {
      if (annotations != null) {
         getConstantPoolItemForRuntimeVisibleAnnotationsAttribute();
         return 8 + annotations.getSize();
      }

      return 0;
   }

   private int getConstantPoolItemForRuntimeVisibleAnnotationsAttribute() {
      return cp.newUTF8("RuntimeVisibleAnnotations");
   }

   final void putAnnotations(ByteVector out) {
      if (annotations != null) {
         int item = getConstantPoolItemForRuntimeVisibleAnnotationsAttribute();
         out.putShort(item);
         annotations.put(out);
      }
   }
}

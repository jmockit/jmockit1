package mockit.external.asm;

import javax.annotation.*;

final class FieldReader extends AnnotatedReader
{
   @Nonnull private final ClassVisitor cv;

   FieldReader(@Nonnull ClassReader cr) {
      super(cr);
      cv = cr.cv;
   }

   @Nonnegative
   int readFields(@Nonnegative int codeIndex) {
      int fieldCount = readUnsignedShort(codeIndex);
      codeIndex += 2;

      for (int i = fieldCount; i > 0; i--) {
         codeIndex = readField(codeIndex);
      }

      return codeIndex;
   }

   /**
    * Reads a field and makes the given visitor visit it.
    *
    * @param codeIndex the start offset of the field in the class file.
    * @return the offset of the first byte following the field in the class.
    */
   @Nonnegative
   private int readField(@Nonnegative int codeIndex) {
      // Reads the field declaration.
      int access = readUnsignedShort(codeIndex);
      String name = readNonnullUTF8(codeIndex + 2);
      String desc = readNonnullUTF8(codeIndex + 4);
      codeIndex += 6;

      // Reads the field attributes.
      String signature = null;
      int anns = 0;
      Object value = null;

      for (int attributeCount = readUnsignedShort(codeIndex); attributeCount > 0; attributeCount--) {
         String attrName = readNonnullUTF8(codeIndex + 2);

         if ("ConstantValue".equals(attrName)) {
            int item = readUnsignedShort(codeIndex + 8);
            value = item == 0 ? null : readConst(item);
         }
         else if ("Signature".equals(attrName)) {
            signature = readNonnullUTF8(codeIndex + 8);
         }
         else if ("Deprecated".equals(attrName)) {
            access = Access.asDeprecated(access);
         }
         else if ("Synthetic".equals(attrName)) {
            access = Access.asSynthetic(access);
         }
         else if ("RuntimeVisibleAnnotations".equals(attrName)) {
            anns = codeIndex + 8;
         }

         codeIndex += 6 + readInt(codeIndex + 4);
      }

      codeIndex += 2;

      FieldVisitor fv = cv.visitField(access, name, desc, signature, value);

      if (fv != null) {
         readAnnotations(fv, anns);
         fv.visitEnd();
      }

      return codeIndex;
   }

   private void readAnnotations(@Nonnull FieldVisitor fv, @Nonnegative int anns) {
      if (anns != 0) {
         for (int annotationCount = readUnsignedShort(anns), v = anns + 2; annotationCount > 0; annotationCount--) {
            String desc = readNonnullUTF8(v);
            AnnotationVisitor av = fv.visitAnnotation(desc);
            v = annotationReader.readNamedAnnotationValues(v + 2, av);
         }
      }
   }
}

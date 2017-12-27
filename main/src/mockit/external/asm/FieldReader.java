package mockit.external.asm;

import javax.annotation.*;

final class FieldReader extends AnnotatedReader
{
   @Nonnull private final ClassVisitor cv;
   @Nullable private String signature;
   @Nonnegative private int annotationCodeIndex;
   private int access;

   FieldReader(@Nonnull ClassReader cr) {
      super(cr);
      cv = cr.cv;
   }

   /**
    * Reads each field and makes the given visitor visit it.
    *
    * @return the offset of the first byte following the last field in the class.
    */
   @Nonnegative
   int readFields() {
      for (int fieldCount = readUnsignedShort(); fieldCount > 0; fieldCount--) {
         readField();
      }

      return codeIndex;
   }

   private void readField() {
      access = readUnsignedShort();
      String name = readNonnullUTF8();
      String desc = readNonnullUTF8();
      Object constantValue = readFieldAttributes();

      FieldVisitor fv = cv.visitField(access, name, desc, signature, constantValue);

      if (fv != null) {
         readAnnotations(fv);
         fv.visitEnd();
      }
   }

   @Nullable
   private Object readFieldAttributes() {
      signature = null;
      annotationCodeIndex = 0;
      int attributeCount = readUnsignedShort();
      Object constantValue = null;

      while (attributeCount > 0) {
         String attrName = readNonnullUTF8();
         int codeOffset = readInt();

         if ("ConstantValue".equals(attrName)) {
            int item = readUnsignedShort(codeIndex);
            constantValue = item == 0 ? null : readConst(item);
         }
         else if ("Signature".equals(attrName)) {
            signature = readNonnullUTF8(codeIndex);
         }
         else if ("RuntimeVisibleAnnotations".equals(attrName)) {
            annotationCodeIndex = codeIndex;
         }
         else {
            readAccessAttribute(attrName);
         }

         codeIndex += codeOffset;
         attributeCount--;
      }

      return constantValue;
   }

   private void readAccessAttribute(@Nonnull String attrName) {
      if ("Deprecated".equals(attrName)) {
         access = Access.asDeprecated(access);
      }
      else if ("Synthetic".equals(attrName)) {
         access = Access.asSynthetic(access);
      }
   }

   private void readAnnotations(@Nonnull FieldVisitor fv) {
      if (annotationCodeIndex > 0) {
         int annotationCount = readUnsignedShort(annotationCodeIndex);
         annotationCodeIndex += 2;

         while (annotationCount > 0) {
            String desc = readNonnullUTF8(annotationCodeIndex);
            annotationCodeIndex += 2;

            AnnotationVisitor av = fv.visitAnnotation(desc);
            annotationCodeIndex = annotationReader.readNamedAnnotationValues(annotationCodeIndex, av);

            annotationCount--;
         }
      }
   }
}

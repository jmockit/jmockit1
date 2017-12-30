package mockit.external.asm;

import javax.annotation.*;

final class FieldReader extends AnnotatedReader
{
   @Nonnull private final ClassVisitor cv;

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
      annotationsCodeIndex = 0;
      Object constantValue = null;

      for (int attributeCount = readUnsignedShort(); attributeCount > 0; attributeCount--) {
         String attrName = readNonnullUTF8();
         int codeOffsetToNextAttribute = readInt();

         if ("ConstantValue".equals(attrName)) {
            int constItemIndex = readUnsignedShort();
            constantValue = constItemIndex == 0 ? null : readConst(constItemIndex);
            continue;
         }

         if (readSignature(attrName)) {
            continue;
         }

         if (!readRuntimeVisibleAnnotations(attrName)) {
            readMarkerAttributes(attrName);
         }

         codeIndex += codeOffsetToNextAttribute;
      }

      return constantValue;
   }
}

package mockit.external.asm;

import javax.annotation.*;

final class FieldReader extends AnnotatedReader
{
   @Nonnull private final ClassVisitor cv;
   @Nullable private Object constantValue;

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
      constantValue = null;

      readAttributes();

      FieldVisitor fv = cv.visitField(access, name, desc, signature, constantValue);

      if (fv != null) {
         readAnnotations(fv);
         fv.visitEnd();
      }
   }

   @Nullable @Override
   Boolean readAttribute(@Nonnull String attributeName) {
      if ("ConstantValue".equals(attributeName)) {
         int constItemIndex = readUnsignedShort();

         if (constItemIndex > 0) {
            constantValue = readConst(constItemIndex);
         }

         return true;
      }

      return null;
   }
}

package mockit.external.asm;

import javax.annotation.*;

final class SourceInfoWriter extends AttributeWriter
{
   /**
    * The index of the constant pool item that contains the name of the source file from which this class was compiled.
    */
   @Nonnegative private int sourceFileIndex;

   SourceInfoWriter(@Nonnull ConstantPoolGeneration cp) { super(cp); }

   void setSourceFileName(@Nullable String fileName) {
      if (fileName != null) {
         setAttribute("SourceFile");
         sourceFileIndex = cp.newUTF8(fileName);
      }
   }

   @Nonnegative @Override
   int getSize() { return sourceFileIndex == 0 ? 0 : 8; }

   @Override
   void put(@Nonnull ByteVector out) {
      if (sourceFileIndex > 0) {
         super.put(out);
         out.putShort(sourceFileIndex);
      }
   }
}

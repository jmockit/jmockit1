package mockit.external.asm;

import javax.annotation.*;

final class SourceInfo
{
   @Nonnull private final ConstantPoolGeneration cp;

   /**
    * The index of the constant pool item that contains the name of the source file from which this class was compiled.
    */
   @Nonnegative private int sourceFile;

   SourceInfo(@Nonnull ConstantPoolGeneration cp) { this.cp = cp; }

   void add(@Nullable String file) {
      if (file != null) {
         sourceFile = cp.newUTF8(file);
      }
   }

   @Nonnegative
   int getAttributeCount() { return sourceFile == 0 ? 0 : 1; }

   @Nonnegative
   int getSize() {
      if (sourceFile == 0) {
         return 0;
      }

      cp.newUTF8("SourceFile");
      return 8;
   }

   void put(@Nonnull ByteVector out) {
      if (sourceFile != 0) {
         out.putShort(cp.newUTF8("SourceFile")).putInt(2).putShort(sourceFile);
      }
   }
}

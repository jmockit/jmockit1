package mockit.asm;

import javax.annotation.*;

import mockit.asm.constantPool.*;

/**
 * Writes out into the constant pool the item index containing the name of the source file from which the class was compiled.
 */
final class SourceFileWriter extends AttributeWriter
{
   @Nonnegative private final int sourceFileIndex;

   SourceFileWriter(@Nonnull ConstantPoolGeneration cp, @Nonnull String fileName) {
      super(cp, "SourceFile");
      sourceFileIndex = cp.newUTF8(fileName);
   }

   @Nonnegative @Override
   int getSize() { return 8; }

   @Override
   void put(@Nonnull ByteVector out) {
      super.put(out);
      out.putShort(sourceFileIndex);
   }
}

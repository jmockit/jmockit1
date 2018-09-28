package mockit.asm;

import javax.annotation.*;

import mockit.asm.constantPool.*;
import mockit.asm.util.*;

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
   public int getSize() { return 8; }

   @Override
   public void put(@Nonnull ByteVector out) {
      super.put(out);
      out.putShort(sourceFileIndex);
   }
}

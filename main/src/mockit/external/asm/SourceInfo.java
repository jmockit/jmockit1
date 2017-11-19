package mockit.external.asm;

import javax.annotation.*;

final class SourceInfo
{
   @Nonnull private final ConstantPoolGeneration cp;

   /**
    * The index of the constant pool item that contains the name of the source file from which this class was compiled.
    */
   @Nonnegative private int sourceFile;

   /**
    * The SourceDebug attribute of this class.
    */
   @Nullable private ByteVector sourceDebug;

   SourceInfo(@Nonnull ConstantPoolGeneration cp) { this.cp = cp; }

   void add(@Nullable String file, @Nullable String debug) {
      if (file != null) {
         sourceFile = cp.newUTF8(file);
      }

      if (debug != null) {
         sourceDebug = new ByteVector().encodeUTF8(debug, 0, Integer.MAX_VALUE);
      }
   }

   @Nonnegative
   int getAttributeCount() {
      int count = 0;
      if (sourceFile != 0) count++;
      if (sourceDebug != null) count++;
      return count;
   }

   @Nonnegative
   int getSize() {
      int size = 0;

      if (sourceFile != 0) {
         size += 8;
         cp.newUTF8("SourceFile");
      }

      if (sourceDebug != null) {
         size += sourceDebug.length + 6;
         cp.newUTF8("SourceDebugExtension");
      }

      return size;
   }

   void put(@Nonnull ByteVector out) {
      if (sourceFile != 0) {
         out.putShort(cp.newUTF8("SourceFile")).putInt(2).putShort(sourceFile);
      }

      if (sourceDebug != null) {
         out.putShort(cp.newUTF8("SourceDebugExtension")).putInt(sourceDebug.length);
         out.putByteVector(sourceDebug);
      }
   }
}

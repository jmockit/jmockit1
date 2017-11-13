package mockit.external.asm;

final class SourceInfo
{
   private final ClassWriter cw;

   /**
    * The index of the constant pool item that contains the name of the source file from which this class was compiled.
    */
   private int sourceFile;

   /**
    * The SourceDebug attribute of this class.
    */
   private ByteVector sourceDebug;

   SourceInfo(ClassWriter cw) { this.cw = cw; }

   void add(String file, String debug) {
      if (file != null) {
         sourceFile = cw.newUTF8(file);
      }

      if (debug != null) {
         sourceDebug = new ByteVector().encodeUTF8(debug, 0, Integer.MAX_VALUE);
      }
   }

   int getAttributeCount() {
      int count = 0;
      if (sourceFile != 0) count++;
      if (sourceDebug != null) count++;
      return count;
   }

   int getSize() {
      int size = 0;

      if (sourceFile != 0) {
         size += 8;
         cw.newUTF8("SourceFile");
      }

      if (sourceDebug != null) {
         size += sourceDebug.length + 6;
         cw.newUTF8("SourceDebugExtension");
      }

      return size;
   }

   void put(ByteVector out) {
      if (sourceFile != 0) {
         out.putShort(cw.newUTF8("SourceFile")).putInt(2).putShort(sourceFile);
      }

      if (sourceDebug != null) {
         out.putShort(cw.newUTF8("SourceDebugExtension")).putInt(sourceDebug.length);
         out.putByteVector(sourceDebug);
      }
   }
}

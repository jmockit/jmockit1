package mockit.external.asm;

final class LineNumbers
{
   private final ClassWriter cw;

   /**
    * Number of entries in the LineNumberTable attribute.
    */
   private int lineNumberCount;

   /**
    * The LineNumberTable attribute.
    */
   private ByteVector lineNumber;

   LineNumbers(ClassWriter cw) { this.cw = cw; }

   void addLineNumber(int line, Label start) {
      if (lineNumber == null) {
         lineNumber = new ByteVector();
      }

      lineNumberCount++;
      lineNumber.putShort(start.position);
      lineNumber.putShort(line);
   }

   boolean hasLineNumbers() { return lineNumber != null; }

   int getSize() { return lineNumber == null ? 0 : 8 + lineNumber.length; }

   int getSizeWhileAddingConstantPoolItem() {
      if (lineNumber != null) {
         cw.newUTF8("LineNumberTable");
      }

      return getSize();
   }

   void put(ByteVector out) {
      if (lineNumber != null) {
         out.putShort(cw.newUTF8("LineNumberTable"));
         out.putInt(lineNumber.length + 2).putShort(lineNumberCount);
         out.putByteVector(lineNumber);
      }
   }
}

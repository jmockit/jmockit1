package mockit.external.asm;

final class LineNumbers
{
   private final ConstantPoolGeneration cp;

   /**
    * Number of entries in the LineNumberTable attribute.
    */
   private int lineNumberCount;

   /**
    * The LineNumberTable attribute.
    */
   private ByteVector lineNumber;

   LineNumbers(ConstantPoolGeneration cp) { this.cp = cp; }

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
         cp.newUTF8("LineNumberTable");
      }

      return getSize();
   }

   void put(ByteVector out) {
      if (lineNumber != null) {
         out.putShort(cp.newUTF8("LineNumberTable"));
         out.putInt(lineNumber.length + 2).putShort(lineNumberCount);
         out.putByteVector(lineNumber);
      }
   }
}

package mockit.external.asm;

import javax.annotation.*;

final class LineNumberWriter extends AttributeWriter
{
   /**
    * Number of entries in the <tt>LineNumberTable</tt> attribute.
    */
   @Nonnegative private int lineNumberCount;

   /**
    * The <tt>LineNumberTable</tt> attribute.
    */
   @Nullable private ByteVector lineNumbers;

   LineNumberWriter(@Nonnull ConstantPoolGeneration cp) { super(cp); }

   void addLineNumber(@Nonnegative int line, @Nonnull Label start) {
      if (lineNumbers == null) {
         setAttribute("LineNumberTable");
         lineNumbers = new ByteVector();
      }

      lineNumberCount++;
      lineNumbers.putShort(start.position);
      lineNumbers.putShort(line);
   }

   boolean hasLineNumbers() { return lineNumbers != null; }

   @Nonnegative @Override
   int getSize() { return lineNumbers == null ? 0 : 8 + lineNumbers.length; }

   @Override
   void put(@Nonnull ByteVector out) {
      if (lineNumbers != null) {
         put(out, lineNumbers.length + 2);
         out.putShort(lineNumberCount);
         out.putByteVector(lineNumbers);
      }
   }
}

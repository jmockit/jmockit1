package mockit.asm.methods;

import javax.annotation.*;

import mockit.asm.*;
import mockit.asm.constantPool.*;
import mockit.asm.util.*;

/**
 * Writes the bytecode for the "LineNumberTable" method code attribute.
 */
final class LineNumberTableWriter extends AttributeWriter
{
   /**
    * Number of entries in the <tt>LineNumberTable</tt> attribute.
    */
   @Nonnegative private int lineNumberCount;

   /**
    * The <tt>LineNumberTable</tt> attribute.
    */
   @Nullable private ByteVector lineNumbers;

   LineNumberTableWriter(@Nonnull ConstantPoolGeneration cp) { super(cp); }

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
   public int getSize() { return lineNumbers == null ? 0 : 8 + lineNumbers.getLength(); }

   @Override
   public void put(@Nonnull ByteVector out) {
      if (lineNumbers != null) {
         put(out, 2 + lineNumbers.getLength());
         out.putShort(lineNumberCount);
         out.putByteVector(lineNumbers);
      }
   }
}

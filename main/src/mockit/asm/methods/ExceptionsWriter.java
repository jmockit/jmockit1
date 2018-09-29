package mockit.asm.methods;

import javax.annotation.*;

import mockit.asm.constantPool.*;
import mockit.asm.util.*;

/**
 * Stores the exceptions that can be thrown by a method/constructor, and writes it to the "Exceptions" attribute.
 * For each thrown exception, stores the index of the constant pool item containing the internal name of the thrown exception class.
 */
final class ExceptionsWriter extends AttributeWriter
{
   @Nonnull private final int[] exceptionIndices;

   ExceptionsWriter(@Nonnull ConstantPoolGeneration cp, @Nonnull String[] exceptionTypeDescs) {
      super(cp, "Exceptions");
      int n = exceptionTypeDescs.length;
      exceptionIndices = new int[n];

      for (int i = 0; i < n; i++) {
         exceptionIndices[i] = cp.newClass(exceptionTypeDescs[i]);
      }
   }

   @Nonnegative @Override
   public int getSize() { return 8 + 2 * exceptionIndices.length; }

   @Override
   public void put(@Nonnull ByteVector out) {
      int n = exceptionIndices.length;
      put(out, 2 + 2 * n);
      out.putShort(n);

      for (int exceptionIndex : exceptionIndices) {
         out.putShort(exceptionIndex);
      }
   }
}

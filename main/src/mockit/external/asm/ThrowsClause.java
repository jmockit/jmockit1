package mockit.external.asm;

import javax.annotation.*;

/**
 * Stores the exceptions that can be thrown by a method/constructor. For each thrown exception, stores the index of the
 * constant pool item containing the internal name of the thrown exception class. Provides the bytecode
 * {@linkplain #getSize() size}  of the "Exceptions" attribute, and allows it to be
 * {@linkplain #put(ByteVector) written out}.
 */
final class ThrowsClause
{
   @Nonnull private final int[] exceptions;
   @Nonnegative private final int attributeIndex;

   ThrowsClause(@Nonnull ConstantPoolGeneration cp, @Nonnull String[] exceptionTypeDescs) {
      int n = exceptionTypeDescs.length;
      exceptions = new int[n];

      for (int i = 0; i < n; i++) {
         String exceptionTypeDesc = exceptionTypeDescs[i];
         exceptions[i] = cp.newClass(exceptionTypeDesc);
      }

      attributeIndex = cp.newUTF8("Exceptions");
   }

   @Nonnegative int getCount() { return exceptions.length; }
   @Nonnegative int getExceptionIndex(@Nonnegative int exceptionIndex) { return exceptions[exceptionIndex]; }
   @Nonnegative int getSize() { return 8 + 2 * exceptions.length; }

   void put(@Nonnull ByteVector out) {
      int[] exceptions = this.exceptions;
      int n = exceptions.length;
      out.putShort(attributeIndex).putInt(2 * n + 2);
      out.putShort(n);

      for (int exception : exceptions) {
         out.putShort(exception);
      }
   }
}

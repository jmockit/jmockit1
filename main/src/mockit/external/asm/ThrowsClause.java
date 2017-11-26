package mockit.external.asm;

import javax.annotation.*;

final class ThrowsClause
{
   @Nonnull private final ConstantPoolGeneration cp;

   /**
    * Number of exceptions that can be thrown by the method/constructor.
    */
   @Nonnegative private final int exceptionCount;

   /**
    * The exceptions that can be thrown by the method/constructor. More precisely, this array contains the indexes of
    * the constant pool items that contain the internal names of these exception classes.
    */
   @Nullable private final int[] exceptions;

   ThrowsClause(@Nonnull ConstantPoolGeneration cp, @Nullable String[] exceptionTypeDescs) {
      this.cp = cp;

      if (exceptionTypeDescs == null) {
         exceptionCount = 0;
         exceptions = null;
         return;
      }

      int n = exceptionTypeDescs.length;
      exceptionCount = n;
      exceptions = n == 0 ? null : new int[n];

      for (int i = 0; i < n; i++) {
         exceptions[i] = cp.newClass(exceptionTypeDescs[i]);
      }
   }

   @Nonnegative int getExceptionCount() { return exceptionCount; }

   boolean hasExceptions() { return exceptionCount > 0; }

   int getExceptionIndex(int i) {
      //noinspection ConstantConditions
      return exceptions[i];
   }

   @Nonnegative
   int getSize() {
      if (exceptionCount > 0) {
         cp.newUTF8("Exceptions");
         return 8 + 2 * exceptionCount;
      }

      return 0;
   }

   void put(@Nonnull ByteVector out) {
      int n = exceptionCount;

      if (n > 0) {
         @SuppressWarnings("ConstantConditions") @Nonnull int[] exceptions = this.exceptions;

         out.putShort(cp.newUTF8("Exceptions")).putInt(2 * n + 2);
         out.putShort(n);

         for (int i = 0; i < n; i++) {
            out.putShort(exceptions[i]);
         }
      }
   }
}

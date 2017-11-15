package mockit.external.asm;

final class ThrowsClause
{
   private final ConstantPoolGeneration cp;

   /**
    * Number of exceptions that can be thrown by the method/constructor.
    */
   private final int exceptionCount;

   /**
    * The exceptions that can be thrown by the method/constructor. More precisely, this array contains the indexes of
    * the constant pool items that contain the internal names of these exception classes.
    */
   private final int[] exceptions;

   ThrowsClause(ConstantPoolGeneration cp, String[] exceptions) {
      this.cp = cp;

      int n;

      if (exceptions == null) {
         n = 0;
         exceptionCount = 0;
         this.exceptions = null;
      }
      else {
         n = exceptions.length;
         exceptionCount = n;
         this.exceptions = n == 0 ? null : new int[n];
      }

      for (int i = 0; i < n; ++i) {
         this.exceptions[i] = cp.newClass(exceptions[i]);
      }
   }

   int getExceptionCount() { return exceptionCount; }
   boolean hasExceptions() { return exceptionCount > 0; }
   int getExceptionIndex(int i) { return exceptions[i]; }

   int getSize() {
      if (exceptionCount > 0) {
         cp.newUTF8("Exceptions");
         return 8 + 2 * exceptionCount;
      }

      return 0;
   }

   void put(ByteVector out) {
      if (exceptionCount > 0) {
         out.putShort(cp.newUTF8("Exceptions")).putInt(2 * exceptionCount + 2);
         out.putShort(exceptionCount);

         for (int i = 0; i < exceptionCount; ++i) {
            out.putShort(exceptions[i]);
         }
      }
   }
}

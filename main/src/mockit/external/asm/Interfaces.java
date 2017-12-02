package mockit.external.asm;

import javax.annotation.*;

final class Interfaces
{
   /**
    * Number of interfaces implemented or extended by this class or interface.
    */
   @Nonnegative private final int interfaceCount;

   /**
    * The interfaces implemented or extended by this class or interface. More precisely, this array contains the indexes
    * of the constant pool items that contain the internal names of these interfaces.
    */
   private final int[] interfaces;

   Interfaces(@Nonnull ConstantPoolGeneration cp, @Nonnull String[] interfaces) {
      int n = interfaceCount = interfaces.length;
      this.interfaces = new int[n];

      for (int i = 0; i < n; i++) {
         this.interfaces[i] = cp.newClass(interfaces[i]);
      }
   }

   @Nonnegative
   int getCount() { return interfaceCount; }

   void put(@Nonnull ByteVector out) {
      for (int i = 0; i < interfaceCount; i++) {
         out.putShort(interfaces[i]);
      }
   }
}

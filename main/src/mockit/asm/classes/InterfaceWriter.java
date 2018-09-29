package mockit.asm.classes;

import javax.annotation.*;

import mockit.asm.constantPool.*;
import mockit.asm.util.*;

/**
 * Writes out the interfaces implemented or extended by the class or interface being written.
 */
final class InterfaceWriter extends AttributeWriter
{
   @Nonnull private final int[] interfaceItemIndices;

   InterfaceWriter(@Nonnull ConstantPoolGeneration cp, @Nonnull String[] interfaceNames) {
      super(cp);

      int n = interfaceNames.length;
      int[] cpItemIndices = new int[n];

      for (int i = 0; i < n; i++) {
         cpItemIndices[i] = cp.newClass(interfaceNames[i]);
      }

      interfaceItemIndices = cpItemIndices;
   }

   @Override
   public int getSize() { return 2 * interfaceItemIndices.length; }

   @Override
   public void put(@Nonnull ByteVector out) {
      out.putShort(interfaceItemIndices.length);

      for (int interfaceItemIndex : interfaceItemIndices) {
         out.putShort(interfaceItemIndex);
      }
   }
}

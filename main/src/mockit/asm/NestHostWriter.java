package mockit.asm;

import javax.annotation.*;

final class NestHostWriter extends AttributeWriter
{
   @Nonnegative private final int nestHostIndex;

   NestHostWriter(@Nonnull ConstantPoolGeneration cp, @Nonnull String nestHost) {
      super(cp, "NestHost");
      nestHostIndex = cp.newClass(nestHost);
   }

   @Nonnegative @Override
   int getSize() {
      return 8;
   }

   @Override
   void put(@Nonnull ByteVector out) {
      super.put(out);
      out.putShort(nestHostIndex);
   }
}

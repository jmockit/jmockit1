package mockit.external.asm;

import javax.annotation.*;

final class EnclosingMethod
{
   @Nonnull final String owner;
   @Nullable final String name;
   @Nullable final String desc;

   EnclosingMethod(@Nonnull BytecodeReader br, @Nonnull char[] c, @Nonnegative int u) {
      //noinspection ConstantConditions
      owner = br.readClass(u + 8, c);
      int item = br.readUnsignedShort(u + 10);

      if (item != 0) {
         name = br.readUTF8Item(item, 0, c);
         desc = br.readUTF8Item(item, 2, c);
      }
      else {
         name = null;
         desc = null;
      }
   }
}

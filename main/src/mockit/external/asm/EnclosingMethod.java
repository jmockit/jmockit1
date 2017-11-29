package mockit.external.asm;

import javax.annotation.*;

final class EnclosingMethod
{
   @Nonnull final String owner;
   @Nullable final String name;
   @Nullable final String desc;

   EnclosingMethod(@Nonnull ClassReader cr, @Nonnegative int u) {
      //noinspection ConstantConditions
      owner = cr.readClass(u + 8);
      int item = cr.readUnsignedShort(u + 10);

      if (item != 0) {
         int nameIndex = cr.items[item];
         char[] c = cr.buf;
         name = cr.readUTF8(nameIndex, c);
         desc = cr.readUTF8(nameIndex + 2, c);
      }
      else {
         name = null;
         desc = null;
      }
   }
}

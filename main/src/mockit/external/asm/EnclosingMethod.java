package mockit.external.asm;

import javax.annotation.*;

final class EnclosingMethod
{
   @Nonnull final String owner;
   @Nullable final String name;
   @Nullable final String desc;

   EnclosingMethod(@Nonnull ClassReader cr, @Nonnegative int codeIndex) {
      owner = cr.readNonnullClass(codeIndex);
      codeIndex += 2;

      int item = cr.readUnsignedShort(codeIndex);

      if (item != 0) {
         int nameIndex = cr.items[item];
         name = cr.readNonnullUTF8(nameIndex);
         nameIndex += 2;
         desc = cr.readNonnullUTF8(nameIndex);
      }
      else {
         name = null;
         desc = null;
      }
   }
}

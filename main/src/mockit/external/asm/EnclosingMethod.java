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

      int itemIndex = cr.readUnsignedShort(codeIndex);

      if (itemIndex > 0) {
         int nameCodeIndex = cr.items[itemIndex];
         name = cr.readNonnullUTF8(nameCodeIndex);
         desc = cr.readNonnullUTF8(nameCodeIndex + 2);
      }
      else {
         name = null;
         desc = null;
      }
   }
}

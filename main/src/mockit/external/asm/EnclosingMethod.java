package mockit.external.asm;

import javax.annotation.*;

final class EnclosingMethod
{
   @Nonnull final String owner;
   @Nullable final String name;
   @Nullable final String desc;

   EnclosingMethod(@Nonnull ClassReader cr) {
      owner = cr.readNonnullClass();

      int nameCodeIndex = cr.readItem();

      if (nameCodeIndex > 0) {
         name = cr.readNonnullUTF8(nameCodeIndex);
         desc = cr.readNonnullUTF8(nameCodeIndex + 2);
      }
      else {
         name = null;
         desc = null;
      }
   }
}

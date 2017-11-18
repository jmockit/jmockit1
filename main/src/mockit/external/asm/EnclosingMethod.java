package mockit.external.asm;

import javax.annotation.*;

final class EnclosingMethod
{
   final String owner;
   final String name;
   final String desc;

   EnclosingMethod(@Nonnull BytecodeReader br, @Nonnull char[] c, @Nonnegative int u) {
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

package mockit.external.asm;

import javax.annotation.*;

class TypeTableItem extends Item
{
   TypeTableItem(@Nonnegative int index) {
      super(index);
   }

   TypeTableItem(@Nonnegative int index, @Nonnull TypeTableItem item) {
      super(index, item);
   }
}

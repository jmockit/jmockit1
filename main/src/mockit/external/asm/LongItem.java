package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.Item.Type.*;

final class LongItem extends LongValueItem
{
   LongItem(@Nonnegative int index) {
      super(index);
      type = LONG;
   }

   LongItem(@Nonnegative int index, @Nonnull LongItem item) { super(index, item); }
}

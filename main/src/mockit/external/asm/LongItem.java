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

   /**
    * Sets the value of this item.
    */
   void set(long value) {
      setValue(value);
   }
}

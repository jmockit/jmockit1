package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.ConstantPoolItemType.*;

final class LongItem extends Item
{
   LongItem(@Nonnegative int index) {
      super(index);
      type = LONG;
   }

   LongItem(@Nonnegative int index, @Nonnull LongItem item) {
      super(index, item);
   }

   /**
    * Sets the value of this item.
    */
   void set(long value) {
      longVal = value;
      hashCode = 0x7FFFFFFF & (LONG + (int) value);
   }
}

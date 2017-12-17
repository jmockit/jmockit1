package mockit.external.asm;

import javax.annotation.*;

class LongValueItem extends Item
{
   /**
    * Value of this item, for a long item.
    */
   long longVal;

   LongValueItem(@Nonnegative int index) { super(index); }

   LongValueItem(@Nonnegative int index, @Nonnull LongValueItem item) {
      super(index, item);
      longVal = item.longVal;
   }

   final void setValue(long value) {
      longVal = value;
      setHashCode((int) value);
   }

   @Override
   final boolean isEqualTo(@Nonnull Item item) {
      return ((LongValueItem) item).longVal == longVal;
   }
}

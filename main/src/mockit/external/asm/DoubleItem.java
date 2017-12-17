package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.Item.Type.*;

final class DoubleItem extends LongValueItem
{
   DoubleItem(@Nonnegative int index) {
      super(index);
      type = DOUBLE;
   }

   DoubleItem(@Nonnegative int index, @Nonnull DoubleItem item) { super(index, item); }

   /**
    * Sets the value of this item.
    */
   void set(double value) {
      long longValue = Double.doubleToRawLongBits(value);
      setValue(longValue);
   }
}

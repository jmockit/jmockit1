package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.Item.Type.*;

final class FloatItem extends IntValueItem
{
   FloatItem(@Nonnegative int index) {
      super(index);
      type = FLOAT;
   }

   FloatItem(@Nonnegative int index, @Nonnull FloatItem item) { super(index, item); }

   /**
    * Sets the value of this item.
    */
   void set(float value) {
      int intValue = Float.floatToRawIntBits(value);
      setValue(intValue);
   }
}

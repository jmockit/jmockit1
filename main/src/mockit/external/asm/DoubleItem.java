package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.ConstantPoolGeneration.ItemType.*;

final class DoubleItem extends Item
{
   DoubleItem(@Nonnegative int index) {
      super(index);
      type = DOUBLE;
   }

   DoubleItem(@Nonnegative int index, @Nonnull DoubleItem item) {
      super(index, item);
   }

   /**
    * Sets the value of this item.
    */
   void set(double value) {
      longVal = Double.doubleToRawLongBits(value);
      setHashCode((int) value);
   }
}

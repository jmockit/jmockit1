package mockit.external.asm;

import static mockit.external.asm.ConstantPoolItemType.*;

final class DoubleItem extends Item
{
   DoubleItem(int index) {
      super(index);
      type = DOUBLE;
   }

   /**
    * Sets the value of this item.
    */
   void set(double value) {
      longVal = Double.doubleToRawLongBits(value);
      hashCode = 0x7FFFFFFF & (DOUBLE + (int) value);
   }
}

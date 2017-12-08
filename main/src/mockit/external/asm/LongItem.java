package mockit.external.asm;

import static mockit.external.asm.ConstantPoolItemType.*;

final class LongItem extends Item
{
   LongItem(int index) {
      super(index);
      type = LONG;
   }

   /**
    * Sets the value of this item.
    */
   void set(long value) {
      longVal = value;
      hashCode = 0x7FFFFFFF & (LONG + (int) value);
   }
}

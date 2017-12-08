package mockit.external.asm;

import static mockit.external.asm.ConstantPoolItemType.*;

final class IntItem extends Item
{
   IntItem(int index) {
      super(index);
      type = INT;
   }

   /**
    * Sets the value of this item.
    */
   void set(int value) {
      intVal = value;
      hashCode = 0x7FFFFFFF & (INT + value);
   }
}

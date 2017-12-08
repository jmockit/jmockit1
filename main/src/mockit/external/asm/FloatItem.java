package mockit.external.asm;

import static mockit.external.asm.ConstantPoolItemType.*;

final class FloatItem extends Item
{
   FloatItem(int index) {
      super(index);
      type = FLOAT;
   }

   /**
    * Sets the value of this item.
    */
   void set(float value) {
      intVal = Float.floatToRawIntBits(value);
      hashCode = 0x7FFFFFFF & (FLOAT + (int) value);
   }
}

package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.ConstantPoolGeneration.ItemType.*;

final class FloatItem extends Item
{
   FloatItem(@Nonnegative int index) {
      super(index);
      type = FLOAT;
   }

   FloatItem(@Nonnegative int index, @Nonnull FloatItem item) {
      super(index, item);
   }

   /**
    * Sets the value of this item.
    */
   void set(float value) {
      intVal = Float.floatToRawIntBits(value);
      hashCode = 0x7FFFFFFF & (FLOAT + (int) value);
   }
}

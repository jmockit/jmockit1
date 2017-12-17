package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.ConstantPoolItemType.*;

final class IntItem extends Item
{
   IntItem(@Nonnegative int index) {
      super(index);
      type = INT;
   }

   IntItem(@Nonnegative int index, @Nonnull IntItem item) {
      super(index, item);
   }

   /**
    * Sets the value of this item.
    */
   void set(int value) {
      intVal = value;
      hashCode = 0x7FFFFFFF & (INT + value);
   }
}

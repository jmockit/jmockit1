package mockit.external.asm;

import javax.annotation.*;

class IntValueItem extends Item
{
   /**
    * Value of this item, for an integer item.
    */
   int intVal;

   IntValueItem(@Nonnegative int index) { super(index); }

   IntValueItem(@Nonnegative int index, @Nonnull IntValueItem item) {
      super(index, item);
      intVal = item.intVal;
   }

   final void setValue(int value) {
      intVal = value;
      setHashCode(value);
   }

   @Override
   final boolean isEqualTo(@Nonnull Item item) {
      return ((IntValueItem) item).intVal == intVal;
   }
}

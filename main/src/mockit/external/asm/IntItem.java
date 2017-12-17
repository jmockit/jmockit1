package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.Item.Type.*;

final class IntItem extends IntValueItem
{
   IntItem(@Nonnegative int index) {
      super(index);
      type = INT;
   }

   IntItem(@Nonnegative int index, @Nonnull IntItem item) { super(index, item); }

   /**
    * Sets the value of this item.
    */
   void set(int value) {
      setValue(value);
   }
}

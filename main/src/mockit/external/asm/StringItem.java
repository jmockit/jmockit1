package mockit.external.asm;

import javax.annotation.*;

final class StringItem extends Item
{
   StringItem(@Nonnegative int index) {
      super(index);
   }

   StringItem(@Nonnegative int index, @Nonnull StringItem item) {
      super(index, item);
   }

   /**
    * Sets this string item value.
    */
   void set(int type, @Nonnull String strVal) {
      this.type = type;
      strVal1 = strVal;
      hashCode = 0x7FFFFFFF & (type + strVal.hashCode());
   }
}

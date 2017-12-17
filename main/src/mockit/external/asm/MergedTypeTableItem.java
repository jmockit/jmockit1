package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.TypeTableItem.SpecialType.MERGED;

final class MergedTypeTableItem extends TypeTableItem
{
   MergedTypeTableItem(@Nonnegative int index) {
      super(index);
      type = MERGED;
   }

   MergedTypeTableItem(@Nonnegative int index, @Nonnull MergedTypeTableItem item) {
      super(index, item);
   }

   /**
    * Sets the types of this merged type table item.
    *
    * @param type1 index of an internal name in the type table.
    * @param type2 index of an internal name in the type table.
    */
   void set(@Nonnegative int type1, @Nonnegative int type2) {
      longVal = type1 | ((long) type2 << 32);
      hashCode = 0x7FFFFFFF & (MERGED + type1 + type2);
   }

   @Override
   boolean isEqualTo(@Nonnull Item item) {
      return item.longVal == longVal;
   }
}

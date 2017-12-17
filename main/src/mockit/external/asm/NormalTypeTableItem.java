package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.TypeTableItem.SpecialType.NORMAL;

final class NormalTypeTableItem extends TypeTableItem
{
   NormalTypeTableItem(@Nonnegative int index) {
      super(index);
      type = NORMAL;
   }

   NormalTypeTableItem(@Nonnegative int index, @Nonnull NormalTypeTableItem item) {
      super(index, item);
   }

   /**
    * Sets the type of this normal type table item.
    *
    * @param type the internal name to be added to the type table.
    */
   void set(@Nonnull String type) {
      typeDesc = type;
      hashCode = 0x7FFFFFFF & (NORMAL + type.hashCode());
   }

   @Override
   boolean isEqualTo(@Nonnull Item item) {
      return ((TypeTableItem) item).typeDesc.equals(typeDesc);
   }
}

package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.Item.SpecialType.*;

final class NormalTypeTableItem extends TypeTableItem
{
   NormalTypeTableItem(@Nonnegative int index) {
      super(index);
      type = NORMAL;
   }

   /**
    * Sets the type of this normal type table item.
    *
    * @param type the internal name to be added to the type table.
    */
   void set(@Nonnull String type) {
      strVal1 = type;
      hashCode = 0x7FFFFFFF & (NORMAL + type.hashCode());
   }
}

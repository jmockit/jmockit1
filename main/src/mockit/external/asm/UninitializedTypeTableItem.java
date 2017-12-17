package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.Item.SpecialType.*;

final class UninitializedTypeTableItem extends TypeTableItem
{
   UninitializedTypeTableItem(@Nonnegative int index) {
      super(index);
      type = UNINIT;
   }

   /**
    * Sets the type and bytecode offset of this uninitialized type table item.
    *
    * @param type   the internal name to be added to the type table.
    * @param offset the bytecode offset of the NEW instruction that created the UNINITIALIZED type value.
    */
   void set(@Nonnull String type, @Nonnegative int offset) {
      intVal = offset;
      strVal1 = type;
      hashCode = 0x7FFFFFFF & (UNINIT + type.hashCode() + offset);
   }
}

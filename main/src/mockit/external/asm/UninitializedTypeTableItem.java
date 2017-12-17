package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.TypeTableItem.SpecialType.UNINIT;

final class UninitializedTypeTableItem extends TypeTableItem
{
   @Nonnegative int offset;

   UninitializedTypeTableItem() { type = UNINIT; }

   UninitializedTypeTableItem(@Nonnegative int index, @Nonnull UninitializedTypeTableItem item) {
      super(index, item);
      offset = item.offset;
   }

   /**
    * Sets the type and bytecode offset of this uninitialized type table item.
    *
    * @param type   the internal name to be added to the type table.
    * @param offset the bytecode offset of the NEW instruction that created the UNINITIALIZED type value.
    */
   void set(@Nonnull String type, @Nonnegative int offset) {
      typeDesc = type;
      this.offset = offset;
      setHashCode(type.hashCode() + offset);
   }

   @Override
   boolean isEqualTo(@Nonnull Item item) {
      UninitializedTypeTableItem other = (UninitializedTypeTableItem) item;
      return other.offset == offset && other.typeDesc.equals(typeDesc);
   }
}

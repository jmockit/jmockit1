package mockit.external.asm;

import javax.annotation.*;

abstract class TypeTableItem extends Item
{
   /**
    * Defines constants for {@link #NORMAL normal}, {@link #UNINIT uninitialized}, and {@link #MERGED merged} special
    * item types stored in the {@linkplain ConstantPoolGeneration#typeTable constant pool's type table}, instead of the
    * constant pool, in order to avoid clashes with normal constant pool items in the
    * {@linkplain ConstantPoolGeneration#items constant pool's hash table}.
    */
   interface SpecialType
   {
      int NORMAL = 30;
      int UNINIT = 31;
      int MERGED = 32;
   }

   @Nonnull String typeDesc;

   TypeTableItem() {
      super(0);
      typeDesc = "";
   }

   TypeTableItem(@Nonnegative int index, @Nonnull TypeTableItem item) {
      super(index, item);
      typeDesc = item.typeDesc;
   }
}

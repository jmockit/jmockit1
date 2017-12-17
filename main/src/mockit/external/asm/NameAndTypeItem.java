package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.ConstantPoolItemType.*;

final class NameAndTypeItem extends Item
{
   NameAndTypeItem(@Nonnegative int index) {
      super(index);
      type = NAME_TYPE;
   }

   NameAndTypeItem(@Nonnegative int index, @Nonnull NameAndTypeItem item) {
      super(index, item);
   }

   /**
    * Sets the values of this name and type item.
    */
   void set(@Nonnull String name, @Nonnull String desc) {
      strVal1 = name;
      strVal2 = desc;
      hashCode = 0x7FFFFFFF & (NAME_TYPE + name.hashCode() * desc.hashCode());
   }
}

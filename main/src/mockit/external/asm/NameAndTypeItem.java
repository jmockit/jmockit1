package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.ConstantPoolGeneration.ItemType.*;

final class NameAndTypeItem extends Item
{
   @Nonnull String name;
   @Nonnull String desc;

   NameAndTypeItem(@Nonnegative int index) {
      super(index);
      type = NAME_TYPE;
      name = desc = "";
   }

   NameAndTypeItem(@Nonnegative int index, @Nonnull NameAndTypeItem item) {
      super(index, item);
      name = item.name;
      desc = item.desc;
   }

   /**
    * Sets the values of this name and type item.
    */
   void set(@Nonnull String name, @Nonnull String desc) {
      this.name = name;
      this.desc = desc;
      hashCode = 0x7FFFFFFF & (NAME_TYPE + name.hashCode() * desc.hashCode());
   }

   @Override
   boolean isEqualTo(@Nonnull Item item) {
      NameAndTypeItem other = (NameAndTypeItem) item;
      return other.name.equals(name) && other.desc.equals(desc);
   }
}

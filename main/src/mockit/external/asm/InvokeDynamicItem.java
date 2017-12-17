package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.ConstantPoolGeneration.ItemType.*;

final class InvokeDynamicItem extends Item
{
   @Nonnull String name;
   @Nonnull String desc;

   InvokeDynamicItem(@Nonnegative int index) {
      super(index);
      type = INDY;
      name = desc = "";
   }

   InvokeDynamicItem(@Nonnegative int index, @Nonnull InvokeDynamicItem item) {
      super(index, item);
      name = item.name;
      desc = item.desc;
   }

   /**
    * Sets the name, desc, and index of the invoke dynamic item.
    *
    * @param name  invokeDynamic's name.
    * @param desc  invokeDynamic's desc.
    * @param index zero based index into the class attribute "BootstrapMethods".
    */
   void set(@Nonnull String name, @Nonnull String desc, @Nonnegative int index) {
      longVal = index;
      this.name = name;
      this.desc = desc;
      hashCode = 0x7FFFFFFF & (INDY + index * name.hashCode() * desc.hashCode());
   }

   @Override
   boolean isEqualTo(@Nonnull Item item) {
      InvokeDynamicItem other = (InvokeDynamicItem) item;
      return other.longVal == longVal && other.name.equals(name) && other.desc.equals(desc);
   }
}

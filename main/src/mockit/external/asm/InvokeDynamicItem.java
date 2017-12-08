package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.ConstantPoolItemType.INDY;

final class InvokeDynamicItem extends Item
{
   InvokeDynamicItem(@Nonnegative int index) {
      super(index);
      type = INDY;
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
      strVal1 = name;
      strVal2 = desc;
      hashCode = 0x7FFFFFFF & (INDY + index * name.hashCode() * desc.hashCode());
   }
}

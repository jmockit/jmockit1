package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.Item.Type.*;

final class InvokeDynamicItem extends TypeOrMemberItem
{
   @Nonnegative private int bsmIndex;

   InvokeDynamicItem(@Nonnegative int index) {
      super(index);
      type = INDY;
   }

   InvokeDynamicItem(@Nonnegative int index, @Nonnull InvokeDynamicItem item) {
      super(index, item);
      bsmIndex = item.bsmIndex;
   }

   /**
    * Sets the name, desc, and index of the invoke dynamic item.
    *
    * @param name  invokeDynamic's name.
    * @param desc  invokeDynamic's desc.
    * @param index zero based index into the class attribute "BootstrapMethods".
    */
   void set(@Nonnull String name, @Nonnull String desc, @Nonnegative int index) {
      bsmIndex = index;
      setValuesAndHashcode(name, desc, index);
   }

   @Override
   boolean isEqualTo(@Nonnull Item item) {
      InvokeDynamicItem other = (InvokeDynamicItem) item;
      return other.bsmIndex == bsmIndex && isEqualTo(other);
   }
}

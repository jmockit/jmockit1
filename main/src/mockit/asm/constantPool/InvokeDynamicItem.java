package mockit.asm.constantPool;

import javax.annotation.*;

import static mockit.asm.jvmConstants.ConstantPoolTypes.*;

public final class InvokeDynamicItem extends TypeOrMemberItem
{
   @Nonnegative private int bsmIndex;

   public InvokeDynamicItem(@Nonnegative int index) {
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
   public void set(@Nonnull String name, @Nonnull String desc, @Nonnegative int index) {
      bsmIndex = index;
      setValuesAndHashcode(name, desc, index);
   }

   @Override
   boolean isEqualTo(@Nonnull Item item) {
      InvokeDynamicItem other = (InvokeDynamicItem) item;
      return other.bsmIndex == bsmIndex && isEqualTo(other);
   }
}

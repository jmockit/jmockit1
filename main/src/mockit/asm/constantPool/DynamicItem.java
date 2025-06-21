package mockit.asm.constantPool;

import javax.annotation.*;

import mockit.asm.jvmConstants.ConstantPoolTypes;

public final class DynamicItem extends TypeOrMemberItem
{
   @Nonnegative int bsmIndex;

   public DynamicItem(@Nonnegative int index) { super(index); }

   DynamicItem(@Nonnegative int index, @Nonnull DynamicItem item) {
      super(index, item);
      bsmIndex = item.bsmIndex;
   }

   /**
    * Sets the type, name, desc, and index of the constant or invoke dynamic item.
    *
    * @param type  one of {@link ConstantPoolTypes#INDY} or {@link ConstantPoolTypes#CONDY}, for invoke or constant dynamic, respectively
    * @param name  the item name
    * @param desc  the item type descriptor
    * @param index zero based index into the class attribute "BootstrapMethods".
    */
   public void set(int type, @Nonnull String name, @Nonnull String desc, @Nonnegative int index) {
      super.type = type;
      bsmIndex = index;
      setValuesAndHashcode(name, desc, index);
   }

   @Override
   boolean isEqualTo(@Nonnull Item item) {
      DynamicItem other = (DynamicItem) item;
      return other.bsmIndex == bsmIndex && isEqualTo(other);
   }
}
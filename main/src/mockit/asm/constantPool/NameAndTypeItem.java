package mockit.asm.constantPool;

import javax.annotation.*;

import static mockit.asm.jvmConstants.ConstantPoolTypes.*;

public final class NameAndTypeItem extends TypeOrMemberItem
{
   public NameAndTypeItem(@Nonnegative int index) {
      super(index);
      type = NAME_TYPE;
   }

   NameAndTypeItem(@Nonnegative int index, @Nonnull NameAndTypeItem item) { super(index, item); }

   /**
    * Sets the name and type descriptor of this item.
    */
   public void set(@Nonnull String name, @Nonnull String desc) {
      setValuesAndHashcode(name, desc, 1);
   }
}

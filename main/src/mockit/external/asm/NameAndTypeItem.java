package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.Item.Type.*;

final class NameAndTypeItem extends TypeOrMemberItem
{
   NameAndTypeItem(@Nonnegative int index) {
      super(index);
      type = NAME_TYPE;
   }

   NameAndTypeItem(@Nonnegative int index, @Nonnull NameAndTypeItem item) { super(index, item); }

   /**
    * Sets the name and type descriptor of this item.
    */
   void set(@Nonnull String name, @Nonnull String desc) {
      setValuesAndHashcode(name, desc, 1);
   }
}

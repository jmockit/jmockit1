package mockit.external.asm;

import javax.annotation.*;

final class ClassMemberItem extends Item
{
   ClassMemberItem(@Nonnegative int index) {
      super(index);
   }

   ClassMemberItem(@Nonnegative int index, @Nonnull ClassMemberItem item) {
      super(index, item);
   }

   /**
    * Sets the values of this field/method item.
    */
   void set(int type, @Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
      this.type = type;
      strVal1 = owner;
      strVal2 = name;
      strVal3 = desc;
      hashCode = 0x7FFFFFFF & (type + owner.hashCode() * name.hashCode() * desc.hashCode());
   }
}

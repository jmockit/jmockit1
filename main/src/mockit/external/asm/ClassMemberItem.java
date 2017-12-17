package mockit.external.asm;

import javax.annotation.*;

final class ClassMemberItem extends TypeOrMemberItem
{
   @Nonnull String owner;

   ClassMemberItem(@Nonnegative int index) {
      super(index);
      owner = "";
   }

   ClassMemberItem(@Nonnegative int index, @Nonnull ClassMemberItem item) {
      super(index, item);
      owner = item.owner;
   }

   /**
    * Sets the values of this field/method item.
    */
   void set(int type, @Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
      this.type = type;
      this.owner = owner;
      setValuesAndHashcode(name, desc, owner.hashCode());
   }

   @Override
   boolean isEqualTo(@Nonnull Item item) {
      ClassMemberItem other = (ClassMemberItem) item;
      return other.owner.equals(owner) && isEqualTo(other);
   }
}

package mockit.external.asm;

import javax.annotation.*;

final class ClassMemberItem extends Item
{
   @Nonnull String owner;
   @Nonnull String name;
   @Nonnull String desc;

   ClassMemberItem(@Nonnegative int index) {
      super(index);
      owner = name = desc = "";
   }

   ClassMemberItem(@Nonnegative int index, @Nonnull ClassMemberItem item) {
      super(index, item);
      owner = item.owner;
      name = item.name;
      desc = item.desc;
   }

   /**
    * Sets the values of this field/method item.
    */
   void set(int type, @Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
      this.type = type;
      this.owner = owner;
      this.name = name;
      this.desc = desc;
      hashCode = 0x7FFFFFFF & (type + owner.hashCode() * name.hashCode() * desc.hashCode());
   }

   @Override
   boolean isEqualTo(@Nonnull Item item) {
      ClassMemberItem other = (ClassMemberItem) item;
      return other.owner.equals(owner) && other.name.equals(name) && other.desc.equals(desc);
   }
}

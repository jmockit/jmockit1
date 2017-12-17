package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.ConstantPoolItemType.*;

final class HandleItem extends Item
{
   HandleItem(@Nonnegative int index) {
      super(index);
      type = HANDLE_BASE;
   }

   HandleItem(@Nonnegative int index, @Nonnull HandleItem item) {
      super(index, item);
   }

   /**
    * Sets the tag and field/method descriptor of this handle item.
    */
   void set(@Nonnull Handle handle) {
      type = HANDLE_BASE + handle.tag;
      strVal1 = handle.owner;
      strVal2 = handle.name;
      strVal3 = handle.desc;
      hashCode = 0x7FFFFFFF & (HANDLE_BASE + handle.hashCode());
   }
}

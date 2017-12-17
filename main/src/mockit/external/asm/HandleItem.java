package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.ConstantPoolGeneration.ItemType.*;

final class HandleItem extends Item
{
   private Handle handle;

   HandleItem(@Nonnegative int index) {
      super(index);
      type = HANDLE_BASE;
   }

   HandleItem(@Nonnegative int index, @Nonnull HandleItem item) {
      super(index, item);
      handle = item.handle;
   }

   /**
    * Sets the tag and field/method descriptor of this handle item.
    */
   void set(@Nonnull Handle handle) {
      this.handle = handle;
      type = HANDLE_BASE + handle.tag;
      hashCode = 0x7FFFFFFF & (HANDLE_BASE + handle.hashCode());
   }

   @Override
   boolean isEqualTo(@Nonnull Item item) {
      return ((HandleItem) item).handle.equals(handle);
   }
}

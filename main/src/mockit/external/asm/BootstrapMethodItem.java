package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.Item.Type.*;

final class BootstrapMethodItem extends Item
{
   @Nonnegative final int position;

   /**
    * Initializes the new item with the given index, position and hash code.
    *
    * @param position position in byte in the class attribute BootstrapMethods.
    * @param hashCode hashcode of the item. This hashcode is processed from the hashcode of the bootstrap method and the
    *                 hashcode of all bootstrap arguments.
    */
   BootstrapMethodItem(@Nonnegative int index, @Nonnegative int position, int hashCode) {
      super(index);
      this.position = position;
      setHashCode(hashCode);
      type = BSM;
   }

   @Override
   boolean isEqualTo(@Nonnull Item item) { return ((BootstrapMethodItem) item).position == position; }
}

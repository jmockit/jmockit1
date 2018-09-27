package mockit.asm.constantPool;

import javax.annotation.*;

import static mockit.asm.constantPool.Item.Type.*;

public final class BootstrapMethodItem extends Item
{
   @Nonnegative final int position;

   /**
    * Initializes the new item with the given index, position and hash code.
    *
    * @param position position in byte in the class attribute "BootstrapMethods"
    * @param hashCode hashcode of the item, which is processed from the hashcode of the bootstrap method and the hashcode of all bootstrap
    *                 arguments
    */
   public BootstrapMethodItem(@Nonnegative int index, @Nonnegative int position, int hashCode) {
      super(index);
      this.position = position;
      setHashCode(hashCode);
      type = BSM;
   }

   @Override
   boolean isEqualTo(@Nonnull Item item) { return ((BootstrapMethodItem) item).position == position; }
}

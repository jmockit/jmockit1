package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.ConstantPoolItemType.*;

final class BootstrapMethodItem extends Item
{
   BootstrapMethodItem(@Nonnegative int index) {
      super(index);
      type = BSM;
   }

   /**
    * Sets the item's position and hash code.
    *
    * @param position position in byte in the class attribute BootstrapMethods.
    * @param hashCode hashcode of the item. This hashcode is processed from the hashcode of the bootstrap method and the
    *                 hashcode of all bootstrap arguments.
    */
   void set(int position, int hashCode) {
      intVal = position;
      this.hashCode = hashCode;
   }
}

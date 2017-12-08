package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.ConstantPoolItemType.*;

final class ReferenceItem extends Item
{
   ReferenceItem(@Nonnegative int index) {
      super(index);
   }

   /**
    * Sets this item to an item that do not hold a primitive value.
    *
    * @param type    the type of this item.
    * @param strVal1 first part of the value of this item.
    * @param strVal2 second part of the value of this item.
    * @param strVal3 third part of the value of this item.
    */
   void set(int type, @Nonnull String strVal1, @Nullable String strVal2, @Nullable String strVal3) {
      this.type = type;
      this.strVal1 = strVal1;
      this.strVal2 = strVal2;
      this.strVal3 = strVal3;

      switch (type) {
         case CLASS:
            intVal = 0; // intVal of a class must be zero, see visitInnerClass
            // fall through
         case UTF8:
         case STR:
         case MTYPE:
         case SpecialType.NORMAL:
            hashCode = 0x7FFFFFFF & (type + strVal1.hashCode());
            return;
         case NAME_TYPE: {
            //noinspection ConstantConditions
            hashCode = 0x7FFFFFFF & (type + strVal1.hashCode() * strVal2.hashCode());
            return;
         }
         // FIELD|METH|IMETH|HANDLE_BASE + 1..9:
         default:
            //noinspection ConstantConditions
            hashCode = 0x7FFFFFFF & (type + strVal1.hashCode() * strVal2.hashCode() * strVal3.hashCode());
      }
   }
}

package mockit.asm.constantPool;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

public final class ModuleItem extends Item
{
   @Nonnull @SuppressWarnings("NullableProblems") String strVal;

   ModuleItem() {
      super(0);
      strVal = "";
   }

   public ModuleItem(@Nonnegative int index, int type, @Nonnull String strVal) {
      super(index);
      set(type, strVal);
   }

   ModuleItem(@Nonnegative int index, @Nonnull ModuleItem item) {
      super(index, item);
      strVal = item.strVal;
   }

   @Nonnull
   public String getValue() { return strVal; }

   /**
    * Sets this module name value.
    */
   void set(int type, @Nonnull String strVal) {
      this.type = type;
      this.strVal = strVal;
      setHashCode(strVal.hashCode());
   }

   @Override
   boolean isEqualTo(@Nonnull Item item) {
      return ((ModuleItem) item).strVal.equals(strVal);
   }
}

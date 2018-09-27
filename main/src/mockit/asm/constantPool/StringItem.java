package mockit.asm.constantPool;

import javax.annotation.*;

public final class StringItem extends Item
{
   @Nonnull @SuppressWarnings("NullableProblems") String strVal;

   StringItem() {
      super(0);
      strVal = "";
   }

   public StringItem(@Nonnegative int index, int type, @Nonnull String strVal) {
      super(index);
      set(type, strVal);
   }

   StringItem(@Nonnegative int index, @Nonnull StringItem item) {
      super(index, item);
      strVal = item.strVal;
   }

   @Nonnull
   public String getValue() { return strVal; }

   /**
    * Sets this string item value.
    */
   void set(int type, @Nonnull String strVal) {
      this.type = type;
      this.strVal = strVal;
      setHashCode(strVal.hashCode());
   }

   @Override
   boolean isEqualTo(@Nonnull Item item) {
      return ((StringItem) item).strVal.equals(strVal);
   }
}

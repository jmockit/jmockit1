package mockit.asm.constantPool;

import javax.annotation.*;

import static mockit.asm.jvmConstants.ConstantPoolTypes.*;

public final class DoubleItem extends LongValueItem
{
   public DoubleItem(@Nonnegative int index) {
      super(index);
      type = DOUBLE;
   }

   DoubleItem(@Nonnegative int index, @Nonnull DoubleItem item) { super(index, item); }

   /**
    * Sets the value of this item.
    */
   public void set(double value) {
      long longValue = Double.doubleToRawLongBits(value);
      setValue(longValue);
   }
}

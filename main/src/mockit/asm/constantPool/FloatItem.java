package mockit.asm.constantPool;

import javax.annotation.*;

import static mockit.asm.jvmConstants.ConstantPoolTypes.*;

public final class FloatItem extends IntValueItem
{
   public FloatItem(@Nonnegative int index) {
      super(index);
      type = FLOAT;
   }

   FloatItem(@Nonnegative int index, @Nonnull FloatItem item) { super(index, item); }

   /**
    * Sets the value of this item.
    */
   public void set(float value) {
      int intValue = Float.floatToRawIntBits(value);
      setValue(intValue);
   }
}

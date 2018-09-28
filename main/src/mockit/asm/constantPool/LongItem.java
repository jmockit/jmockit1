package mockit.asm.constantPool;

import javax.annotation.*;

import static mockit.asm.jvmConstants.ConstantPoolTypes.*;

public final class LongItem extends LongValueItem
{
   public LongItem(@Nonnegative int index) {
      super(index);
      type = LONG;
   }

   LongItem(@Nonnegative int index, @Nonnull LongItem item) { super(index, item); }
}

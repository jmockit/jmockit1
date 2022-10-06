package mockit.asm.constantPool;

import javax.annotation.*;

import static mockit.asm.jvmConstants.ConstantPoolTypes.*;

public final class IntItem extends IntValueItem
{
   public IntItem(@Nonnegative int index) {
      super(index);
      type = INTEGER;
   }

   IntItem(@Nonnegative int index, @Nonnull IntItem item) { super(index, item); }
}

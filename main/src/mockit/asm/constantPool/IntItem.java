package mockit.asm.constantPool;

import javax.annotation.*;

import static mockit.asm.constantPool.Item.Type.*;

public final class IntItem extends IntValueItem
{
   public IntItem(@Nonnegative int index) {
      super(index);
      type = INT;
   }

   IntItem(@Nonnegative int index, @Nonnull IntItem item) { super(index, item); }
}

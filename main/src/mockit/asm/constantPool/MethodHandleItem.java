package mockit.asm.constantPool;

import javax.annotation.*;

import mockit.asm.*;
import static mockit.asm.constantPool.Item.Type.*;

public final class MethodHandleItem extends Item
{
   private MethodHandle methodHandle;

   public MethodHandleItem(@Nonnegative int index) {
      super(index);
      type = HANDLE_BASE;
   }

   MethodHandleItem(@Nonnegative int index, @Nonnull MethodHandleItem item) {
      super(index, item);
      methodHandle = item.methodHandle;
   }

   /**
    * Sets the type and hash code of this method handle item.
    */
   public void set(@Nonnull MethodHandle methodHandle) {
      this.methodHandle = methodHandle;
      type = HANDLE_BASE;
      setHashCode(methodHandle.hashCode());
      type = HANDLE_BASE + methodHandle.tag;
   }

   @Override
   boolean isEqualTo(@Nonnull Item item) {
      return ((MethodHandleItem) item).methodHandle.equals(methodHandle);
   }
}

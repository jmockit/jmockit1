package mockit.asm.constantPool;

import javax.annotation.*;

import mockit.asm.*;

public class TypeOrMemberItem extends Item
{
   @Nonnull String name;
   @Nonnull String desc;
   @Nonnegative private int argSize;

   TypeOrMemberItem(@Nonnegative int index) {
      super(index);
      name = desc = "";
   }

   TypeOrMemberItem(@Nonnegative int index, @Nonnull TypeOrMemberItem item) {
      super(index, item);
      name = item.name;
      desc = item.desc;
   }

   @Nonnull
   public String getName() { return name; }

   @Nonnull
   public String getDesc() { return desc; }

   /**
    * Sets the name and type descriptor of this item, and computes its hashcode.
    */
   final void setValuesAndHashcode(@Nonnull String name, @Nonnull String desc, @Nonnegative int hashCodeMultiplier) {
      this.name = name;
      this.desc = desc;
      setHashCode(hashCodeMultiplier * name.hashCode() * desc.hashCode());
   }

   @Override
   boolean isEqualTo(@Nonnull Item item) {
      return isEqualTo((TypeOrMemberItem) item);
   }

   final boolean isEqualTo(@Nonnull TypeOrMemberItem item) {
      return item.name.equals(name) && item.desc.equals(desc);
   }

   /**
    * Recovers the stack size variation from this constant pool item, computing and storing it if needed.
    * The {@link #argSize} field stores the sizes of the arguments and of the return value corresponding to <tt>desc</tt>.
    */
   @Nonnegative
   public final int getArgSizeComputingIfNeeded(@Nonnull String methodDesc) {
      int thisArgSize = argSize;

      if (thisArgSize == 0) {
         thisArgSize = JavaType.getArgumentsAndReturnSizes(methodDesc);
         argSize = thisArgSize;
      }

      return thisArgSize;
   }
}

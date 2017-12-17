package mockit.external.asm;

import javax.annotation.*;

class TypeOrMemberItem extends Item
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
    * Recovers the stack size variation from this constant pool item, computing and storing it if needed. The
    * {@link #argSize} field stores the sizes of the arguments and of the return value corresponding to <tt>desc</tt>.
    */
   @Nonnegative
   final int getArgSizeComputingIfNeeded(@Nonnull String desc) {
      int argSize = this.argSize;

      if (argSize == 0) {
         argSize = JavaType.getArgumentsAndReturnSizes(desc);
         this.argSize = argSize;
      }

      return argSize;
   }
}

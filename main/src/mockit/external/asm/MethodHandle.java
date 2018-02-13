package mockit.external.asm;

import javax.annotation.*;

/**
 * A reference to a method.
 */
final class MethodHandle
{
   interface Tag
   {
//    int INVOKEVIRTUAL    = 5;
//    int INVOKESTATIC     = 6;
//    int INVOKESPECIAL    = 7;
//    int NEWINVOKESPECIAL = 8;
      int INVOKEINTERFACE  = 9;
   }

   /**
    * The kind of method designated by this handle. Should be one of the {@link Tag} constants.
    */
   @Nonnegative final int tag;

   /**
    * The internal name of the class that owns the method designated by this handle.
    */
   @Nonnull final String owner;

   /**
    * The name of the method designated by this handle.
    */
   @Nonnull final String name;

   /**
    * The descriptor of the method designated by this handle.
    */
   @Nonnull final String desc;

   /**
    * Initializes a new method handle.
    *
    * @param tag   the kind of method designated by this handle. Must be one of the {@link Tag} constants.
    * @param owner the internal name of the class that owns the method designated by this handle.
    * @param name  the name of the method designated by this handle.
    * @param desc  the descriptor of the method designated by this handle.
    */
   MethodHandle(@Nonnegative int tag, @Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
      this.tag = tag;
      this.owner = owner;
      this.name = name;
      this.desc = desc;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }

      if (!(obj instanceof MethodHandle)) {
         return false;
      }

      MethodHandle h = (MethodHandle) obj;
      return tag == h.tag && owner.equals(h.owner) && name.equals(h.name) && desc.equals(h.desc);
   }

   @Override
   public int hashCode() {
      return tag + owner.hashCode() * name.hashCode() * desc.hashCode();
   }
}

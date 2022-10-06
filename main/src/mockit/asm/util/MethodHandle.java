package mockit.asm.util;

import javax.annotation.*;

/**
 * A reference to a method.
 */
public final class MethodHandle
{
   public interface Tag {
      int TAG_GETFIELD         = 1;
      int TAG_GETSTATIC        = 2;
      int TAG_PUTFIELD         = 3;
      int TAG_PUTSTATIC        = 4;
      int TAG_INVOKEVIRTUAL    = 5;
      int TAG_INVOKESTATIC     = 6;
      int TAG_INVOKESPECIAL    = 7;
      int TAG_NEWINVOKESPECIAL = 8;
      int TAG_INVOKEINTERFACE  = 9;
   }

   /**
    * The kind of method designated by this handle. Should be one of the {@link Tag Tag} constants.
    */
   @Nonnegative public final int tag;

   /**
    * The internal name of the class that owns the method designated by this handle.
    */
   @Nonnull public final String owner;

   /**
    * The name of the method designated by this handle.
    */
   @Nonnull public final String name;

   /**
    * The descriptor of the method designated by this handle.
    */
   @Nonnull public final String desc;

   /**
    * Initializes a new method handle.
    *
    * @param tag   the kind of method designated by this handle. Must be one of the {@link Tag} constants.
    * @param owner the internal name of the class that owns the method designated by this handle.
    * @param name  the name of the method designated by this handle.
    * @param desc  the descriptor of the method designated by this handle.
    */
   public MethodHandle(@Nonnegative int tag, @Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
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

package mockit.asm;

import javax.annotation.*;

/**
 * Holds additional information about a classfile: {@link #signature}, {@link #superName}, {@link #interfaces}, {@link #hostClassName}.
 */
public final class ClassInfo
{
   private static final String[] NO_INTERFACES = {};

   public ClassInfo() {
      interfaces = NO_INTERFACES;
   }

   /**
    * The internal names of the class's interfaces, if any.
    */
   @Nonnull public String[] interfaces;

   /**
    * The internal name of the super class.
    * For interfaces, the super class is {@link Object}. May be <tt>null</tt>, but only for the {@link Object} class.
    */
   @Nullable public String superName;

   /**
    * The generic signature of the class.
    * May be <tt>null</tt> if the class is not a generic one, and does not extend or implement generic classes or interfaces.
    */
   @Nullable public String signature;

   /**
    * The internal name of the host class, if the class is part of a nest (Java 11+ only).
    */
   @Nullable String hostClassName;
}

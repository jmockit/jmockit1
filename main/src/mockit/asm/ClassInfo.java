package mockit.asm;

import javax.annotation.*;

/**
 * Holds additional information about a classfile: {@link #signature}, {@link #superName}, {@link #interfaces}, {@link #hostClassName}.
 */
public final class ClassInfo
{
   private static final String[] NO_INTERFACES = {};

   /**
    * The internal names of the class's interfaces, if any.
    */
   @Nonnull public String[] interfaces = NO_INTERFACES;

   /**
    * The internal name of the super class. For interfaces, the super class is {@link Object}.
    * Is <tt>null</tt> only for the {@link Object} class.
    */
   @Nullable public String superName;

   /**
    * The generic signature of the class.
    * Is <tt>null</tt> when the class is not a generic one, and does not extend or implement generic classes or interfaces.
    */
   @Nullable public String signature;

   /**
    * The name of the source file from which the class was compiled, if available.
    */
   @Nullable public String sourceFileName;

   /**
    * The internal name of the host class, if the class is part of a nest (Java 11+ only).
    */
   @Nullable String hostClassName;
}

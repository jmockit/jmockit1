package mockit.external.asm;

/**
 * Defines the JVM and ASM-specific access flags for classes, fields, methods, and method parameters.
 */
public final class Access
{
   // Constants for standard JVM access flags.
   public static final int PUBLIC       = 0x0001; // class, field, method
   public static final int PRIVATE      = 0x0002; // class, field, method
   public static final int PROTECTED    = 0x0004; // class, field, method
   public static final int STATIC       = 0x0008; // field, method
   public static final int FINAL        = 0x0010; // class, field, method, parameter
   public static final int SUPER        = 0x0020; // class
// public static final int SYNCHRONIZED = 0x0020; // method
// public static final int VOLATILE     = 0x0040; // field
   public static final int BRIDGE       = 0x0040; // method
   public static final int VARARGS      = 0x0080; // method
// public static final int TRANSIENT    = 0x0080; // field
   public static final int NATIVE       = 0x0100; // method
   public static final int INTERFACE    = 0x0200; // class
   public static final int ABSTRACT     = 0x0400; // class, method
   public static final int STRICT       = 0x0800; // method
   public static final int SYNTHETIC    = 0x1000; // class, field, method, parameter
   public static final int ANNOTATION   = 0x2000; // class
   public static final int ENUM         = 0x4000; // class(?) field inner
// public static final int MANDATED     = 0x8000; // parameter

   // Constants for pseudo access flags.
   private static final int DEPRECATED  = 0x20000; // class, field, method
   static final int CONSTRUCTOR = 0x80000; // method

   // Other constants /////////////////////////////////////////////////////////////////////////////

   /**
    * To distinguish between the synthetic attribute and the synthetic access flag.
    */
   static final int SYNTHETIC_ATTRIBUTE = 0x40000; // class, field, method

   /**
    * Factor to convert from SYNTHETIC_ATTRIBUTE to SYNTHETIC.
    */
   private static final int TO_SYNTHETIC = SYNTHETIC_ATTRIBUTE / SYNTHETIC;

   // Helper methods //////////////////////////////////////////////////////////////////////////////

   private Access() {}

   static boolean isDeprecated(int access)  { return (access & DEPRECATED) != 0; }
   static boolean isSynthetic(int access)   { return (access & SYNTHETIC) != 0; }
   static boolean isConstructor(int access) { return (access & CONSTRUCTOR) != 0; }

   static int computeFlag(int access, int baseMask) {
      int mask = baseMask | DEPRECATED | SYNTHETIC_ATTRIBUTE | ((access & SYNTHETIC_ATTRIBUTE) / TO_SYNTHETIC);
      return access & ~mask;
   }

   static int asDeprecated(int access) { return access | DEPRECATED; }
   static int asSynthetic(int access)  { return access | SYNTHETIC | SYNTHETIC_ATTRIBUTE; }
}

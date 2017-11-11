package mockit.external.asm;

/**
 * Defines the JVM and ASM-specific access flags for classes, fields, methods, and method parameters.
 */
public final class Access
{
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

   // Pseudo access flags.
   public static final int DEPRECATED  = 0x20000; // class, field, method
   public static final int CONSTRUCTOR = 0x80000; // method

   /**
    * To distinguish between the synthetic attribute and the synthetic access flag.
    */
   public static final int SYNTHETIC_ATTRIBUTE = 0x40000;

   /**
    * Factor to convert from SYNTHETIC_ATTRIBUTE to SYNTHETIC.
    */
   public static final int TO_SYNTHETIC = SYNTHETIC_ATTRIBUTE / SYNTHETIC;

   private Access() {}
}

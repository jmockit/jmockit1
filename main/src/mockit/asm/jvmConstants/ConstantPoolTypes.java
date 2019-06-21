package mockit.asm.jvmConstants;

/**
 * Constants for <a href="https://docs.oracle.com/javase/specs/jvms/se12/html/jvms-4.html#jvms-4.4-140">types of items</a> in the constant
 * pool of a class.
 */
public interface ConstantPoolTypes
{
   int CLASS     =  7; // CONSTANT_Class
   int FIELD     =  9; // CONSTANT_Fieldref
   int METH      = 10; // CONSTANT_Methodref
   int IMETH     = 11; // CONSTANT_InterfaceMethodref
   int STR       =  8; // CONSTANT_String
   int INT       =  3; // CONSTANT_Integer
   int FLOAT     =  4; // CONSTANT_Float
   int LONG      =  5; // CONSTANT_Long
   int DOUBLE    =  6; // CONSTANT_Double
   int NAME_TYPE = 12; // CONSTANT_NameAndType
   int UTF8      =  1; // CONSTANT_Utf8
   int HANDLE    = 15; // CONSTANT_MethodHandle
   int MTYPE     = 16; // CONSTANT_MethodType
   int CONDY     = 17; // CONSTANT_Dynamic
   int INDY      = 18; // CONSTANT_InvokeDynamic

   /**
    * The base value for all CONSTANT_MethodHandle constant pool items.
    * Internally, there are 9 variations of CONSTANT_MethodHandle items, which are stored into 9 different items, of types 21 to 29.
    */
   int HANDLE_BASE = 20;

   /**
    * The type of BootstrapMethod items.
    * These items are stored in a special class attribute named "BootstrapMethods" and not in the constant pool.
    */
   int BSM = 33;
}

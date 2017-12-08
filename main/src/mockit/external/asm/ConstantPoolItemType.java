package mockit.external.asm;

/**
 * Constants for types of items in the constant pool of a class.
 */
interface ConstantPoolItemType
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
   int MTYPE     = 16; // CONSTANT_MethodType
   int HANDLE    = 15; // CONSTANT_MethodHandle
   int INDY      = 18; // CONSTANT_InvokeDynamic

   /**
    * The base value for all CONSTANT_MethodHandle constant pool items.
    * Internally, ASM stores the 9 variations of CONSTANT_MethodHandle into 9 different items.
    */
   int HANDLE_BASE = 20;

   /**
    * The type of BootstrapMethod items. These items are stored in a special class attribute named BootstrapMethods and
    * not in the constant pool.
    */
   int BSM = 33;
}

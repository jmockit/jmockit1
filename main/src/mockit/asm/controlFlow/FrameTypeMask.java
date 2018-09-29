package mockit.asm.controlFlow;

public interface FrameTypeMask
{
   /**
    * Mask to get the dimension of a frame type. This dimension is a signed integer between -8 and 7.
    */
   int DIM = 0xF0000000;

   /**
    * Constant to be added to a type to get a type with one more dimension.
    */
   int ARRAY_OF = 0x10000000;

   /**
    * Constant to be added to a type to get a type with one less dimension.
    */
   int ELEMENT_OF = 0xF0000000;

   /**
    * Mask to get the kind of a frame type.
    *
    * @see #BASE
    * @see #LOCAL
    * @see #STACK
    */
   int KIND = 0xF000000;

   /**
    * Flag used for LOCAL and STACK types. Indicates that if this type happens to be a long or double type (during the
    * computations of input frames), then it must be set to TOP because the second word of this value has been reused to
    * store other data in the basic block. Hence the first word no longer stores a valid long or double value.
    */
   int TOP_IF_LONG_OR_DOUBLE = 0x800000;

   /**
    * Mask to get the value of a frame type.
    */
   int VALUE = 0x7FFFFF;

   /**
    * Mask to get the kind of base types.
    */
   int BASE_KIND = 0xFF00000;

   /**
    * Mask to get the value of base types.
    */
   int BASE_VALUE = 0xFFFFF;

   /**
    * Kind of the types that are not relative to an input stack map frame.
    */
   int BASE = 0x1000000;

   /**
    * Base kind of the base reference types. The BASE_VALUE of such types is an index into the type table.
    */
   int OBJECT = BASE | 0x700000;

   /**
    * Base kind of the uninitialized base types. The BASE_VALUE of such types is an index into the type table (the
    * Item at that index contains both an instruction offset and an internal class name).
    */
   int UNINITIALIZED = BASE | 0x800000;

   /**
    * Kind of the types that are relative to the local variable types of an input stack map frame. The value of such
    * types is a local variable index.
    */
   int LOCAL = 0x2000000;

   /**
    * Kind of the types that are relative to the stack of an input stack map frame. The value of such types is a
    * position relatively to the top of this stack.
    */
   int STACK = 0x3000000;

   /**
    * The TOP type. This is a BASE type.
    */
   int TOP = BASE;

   /**
    * The BOOLEAN type. This is a BASE type mainly used for array types.
    */
   int BOOLEAN = BASE | 9;

   /**
    * The BYTE type. This is a BASE type mainly used for array types.
    */
   int BYTE = BASE | 10;

   /**
    * The CHAR type. This is a BASE type mainly used for array types.
    */
   int CHAR = BASE | 11;

   /**
    * The SHORT type. This is a BASE type mainly used for array types.
    */
   int SHORT = BASE | 12;

   /**
    * The INTEGER type. This is a BASE type.
    */
   int INTEGER = BASE | 1;

   /**
    * The FLOAT type. This is a BASE type.
    */
   int FLOAT = BASE | 2;

   /**
    * The DOUBLE type. This is a BASE type.
    */
   int DOUBLE = BASE | 3;

   /**
    * The LONG type. This is a BASE type.
    */
   int LONG = BASE | 4;

   /**
    * The NULL type. This is a BASE type.
    */
   int NULL = BASE | 5;

   /**
    * The UNINITIALIZED_THIS type. This is a BASE type.
    */
   int UNINITIALIZED_THIS = BASE | 6;
}

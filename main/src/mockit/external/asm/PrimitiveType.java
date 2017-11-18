package mockit.external.asm;

import javax.annotation.*;

final class PrimitiveType extends JavaType
{
   private static final PrimitiveType VOID_TYPE    = new PrimitiveType(Sort.VOID,    'V', 5, 0, 0);
   private static final PrimitiveType BOOLEAN_TYPE = new PrimitiveType(Sort.BOOLEAN, 'Z', 0, 5, 1);
   private static final PrimitiveType CHAR_TYPE    = new PrimitiveType(Sort.CHAR,    'C', 0, 6, 1);
   private static final PrimitiveType BYTE_TYPE    = new PrimitiveType(Sort.BYTE,    'B', 0, 5, 1);
   private static final PrimitiveType SHORT_TYPE   = new PrimitiveType(Sort.SHORT,   'S', 0, 7, 1);
   private static final PrimitiveType INT_TYPE     = new PrimitiveType(Sort.INT,     'I', 0, 0, 1);
   private static final PrimitiveType FLOAT_TYPE   = new PrimitiveType(Sort.FLOAT,   'F', 2, 2, 1);
   private static final PrimitiveType LONG_TYPE    = new PrimitiveType(Sort.LONG,    'J', 1, 1, 2);
   private static final PrimitiveType DOUBLE_TYPE  = new PrimitiveType(Sort.DOUBLE,  'D', 3, 3, 2);

   private final char desc;
   private final int loadOrStoreOffset; // instruction offset for IALOAD or IASTORE
   private final int otherOffset;       // offset for all other instructions
   private final int size;

   /**
    * Constructs a primitive type.
    *
    * @param sort the sort of the primitive type to be constructed.
    * @param desc the primitive type descriptor
    * @param loadOrStoreOffset opcode offset for a IALOAD or IASTORE instruction.
    * @param otherOffset opcode offset for any other instruction.
    * @param size the size in words of the primitive type
    */
   private PrimitiveType(int sort, char desc, int loadOrStoreOffset, int otherOffset, int size) {
      super(sort, 1);
      this.desc = desc;
      this.loadOrStoreOffset = loadOrStoreOffset;
      this.otherOffset = otherOffset;
      this.size = size;
   }

   @Nonnull
   static JavaType getPrimitiveType(@Nonnull Class<?> c) {
      if (c == Integer.TYPE)   return INT_TYPE;
      if (c == Boolean.TYPE)   return BOOLEAN_TYPE;
      if (c == Character.TYPE) return CHAR_TYPE;
      if (c == Double.TYPE)    return DOUBLE_TYPE;
      if (c == Float.TYPE)     return FLOAT_TYPE;
      if (c == Long.TYPE)      return LONG_TYPE;
      if (c == Byte.TYPE)      return BYTE_TYPE;
      if (c == Short.TYPE)     return SHORT_TYPE;
      return VOID_TYPE; // c == Void.TYPE
   }

   @Nullable
   static PrimitiveType getPrimitiveType(char typeCode) {
      switch (typeCode) {
         case 'V': return VOID_TYPE;
         case 'Z': return BOOLEAN_TYPE;
         case 'C': return CHAR_TYPE;
         case 'B': return BYTE_TYPE;
         case 'S': return SHORT_TYPE;
         case 'I': return INT_TYPE;
         case 'F': return FLOAT_TYPE;
         case 'J': return LONG_TYPE;
         case 'D': return DOUBLE_TYPE;
      }

      return null;
   }

   static char getTypeCode(@Nonnull Class<?> c) {
      if (c == Integer.TYPE)   return 'I';
      if (c == Boolean.TYPE)   return 'Z';
      if (c == Byte.TYPE)      return 'B';
      if (c == Character.TYPE) return 'C';
      if (c == Short.TYPE)     return 'S';
      if (c == Double.TYPE)    return 'D';
      if (c == Float.TYPE)     return 'F';
      if (c == Long.TYPE)      return 'J';
      return 'V'; // c == Void.TYPE
   }

   @Nonnull
   public static Class<?> getType(int typeCode) {
      switch (typeCode) {
         case 'I': return int.class;
         case 'Z': return boolean.class;
         case 'D': return double.class;
         case 'J': return long.class;
         case 'C': return char.class;
         case 'B': return byte.class;
         case 'F': return float.class;
         case 'S': return short.class;
         default:  return void.class;
      }
   }

   @Override
   void getDescriptor(@Nonnull StringBuffer buf) {
      buf.append(desc);
   }

   @Nonnull @Override
   public String getClassName() {
      switch (sort) {
         case Sort.INT:     return "int";
         case Sort.BOOLEAN: return "boolean";
         case Sort.DOUBLE:  return "double";
         case Sort.CHAR:    return "char";
         case Sort.FLOAT:   return "float";
         case Sort.LONG:    return "long";
         case Sort.BYTE:    return "byte";
         case Sort.SHORT:   return "short";
         default:           return "void";
      }
   }

   @Override
   public int getSize() { return size; }

   @Override
   public int getOpcode(int opcode) {
      int offset = opcode == Opcodes.IALOAD || opcode == Opcodes.IASTORE ? otherOffset : loadOrStoreOffset;
      return opcode + offset;
   }

   @Override
   public boolean equals(Object o) {
      return this == o || o instanceof PrimitiveType && sort == ((PrimitiveType) o).sort;
   }

   @Override
   public int hashCode() { return 13 * sort; }
}

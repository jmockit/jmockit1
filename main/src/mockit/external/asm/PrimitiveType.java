package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.Opcodes.*;

public final class PrimitiveType extends JavaType
{
   public interface Sort
   {
      int VOID = 0;
      int BOOLEAN = 1;
      int CHAR = 2;
      int BYTE = 3;
      int SHORT = 4;
      int INT = 5;
      int FLOAT = 6;
      int LONG = 7;
      int DOUBLE = 8;
   }

   private static final PrimitiveType VOID    = new PrimitiveType(void.class,    Sort.VOID,    'V', 5, 0, 0, ILOAD, 0);
   private static final PrimitiveType BOOLEAN = new PrimitiveType(boolean.class, Sort.BOOLEAN, 'Z', 0, 5, 1, ILOAD, ICONST_0);
   private static final PrimitiveType CHAR    = new PrimitiveType(char.class,    Sort.CHAR,    'C', 0, 6, 1, ILOAD, ICONST_0);
   private static final PrimitiveType BYTE    = new PrimitiveType(byte.class,    Sort.BYTE,    'B', 0, 5, 1, ILOAD, ICONST_0);
   private static final PrimitiveType SHORT   = new PrimitiveType(short.class,   Sort.SHORT,   'S', 0, 7, 1, ILOAD, ICONST_0);
   private static final PrimitiveType INT     = new PrimitiveType(int.class,     Sort.INT,     'I', 0, 0, 1, ILOAD, ICONST_0);
   private static final PrimitiveType FLOAT   = new PrimitiveType(float.class,   Sort.FLOAT,   'F', 2, 2, 1, FLOAD, FCONST_0);
   private static final PrimitiveType LONG    = new PrimitiveType(long.class,    Sort.LONG,    'J', 1, 1, 2, LLOAD, LCONST_0);
   private static final PrimitiveType DOUBLE  = new PrimitiveType(double.class,  Sort.DOUBLE,  'D', 3, 3, 2, DLOAD, DCONST_0);

   @Nonnull private final Class<?> type;
   private final int sort;
   @Nullable private final String wrapperTypeDesc;
   private final char desc;
   @Nonnegative private final int loadOrStoreOffset; // instruction offset for IALOAD or IASTORE
   @Nonnegative private final int otherOffset;       // offset for all other instructions
   private final int size;
   private final int loadOpcode;
   private final int constOpcode;

   /**
    * Constructs a primitive type.
    *
    * @param sort the sort of the primitive type to be constructed.
    * @param desc the primitive type descriptor
    * @param loadOrStoreOffset opcode offset for a IALOAD or IASTORE instruction.
    * @param otherOffset opcode offset for any other instruction.
    * @param size the size in words of the primitive type
    */
   private PrimitiveType(
      @Nonnull Class<?> type, int sort, char desc, @Nonnegative int loadOrStoreOffset, @Nonnegative int otherOffset,
      @Nonnegative int size, int loadOpcode, int constOpcode
   ) {
      super(1);
      this.type = type;
      this.sort = sort;

      if (desc == 'V') {
         wrapperTypeDesc = null;
      }
      else {
         String typeName = type.getName();
         String simpleWrapperName;

         if (desc == 'C') simpleWrapperName = "Character";
         else if (desc == 'I') simpleWrapperName = "Integer";
         else simpleWrapperName = Character.toUpperCase(typeName.charAt(0)) + typeName.substring(1);

         wrapperTypeDesc = "java/lang/" + simpleWrapperName;
      }

      this.desc = desc;
      this.loadOrStoreOffset = loadOrStoreOffset;
      this.otherOffset = otherOffset;
      this.size = size;
      this.loadOpcode = loadOpcode;
      this.constOpcode = constOpcode;
   }

   @Nonnull
   static PrimitiveType getPrimitiveType(@Nonnull Class<?> aClass) {
      if (aClass == Integer.TYPE)   return INT;
      if (aClass == Boolean.TYPE)   return BOOLEAN;
      if (aClass == Character.TYPE) return CHAR;
      if (aClass == Double.TYPE)    return DOUBLE;
      if (aClass == Float.TYPE)     return FLOAT;
      if (aClass == Long.TYPE)      return LONG;
      if (aClass == Byte.TYPE)      return BYTE;
      if (aClass == Short.TYPE)     return SHORT;
      return VOID; // aClass == Void.TYPE
   }

   @Nullable @SuppressWarnings("OverlyComplexMethod")
   static PrimitiveType getPrimitiveType(char typeCode) {
      switch (typeCode) {
         case 'I': return INT;
         case 'Z': return BOOLEAN;
         case 'C': return CHAR;
         case 'D': return DOUBLE;
         case 'F': return FLOAT;
         case 'J': return LONG;
         case 'B': return BYTE;
         case 'S': return SHORT;
         case 'V': return VOID;
         default:  return null;
      }
   }

   @Nonnull
   public static Class<?> getType(int typeCode) {
      PrimitiveType primitiveType = getPrimitiveType((char) typeCode);
      //noinspection ConstantConditions
      return primitiveType.type;
   }

   /**
    * Maps a {@link Sort} to the corresponding {@link ArrayElementType}.
    */
   public static int getArrayElementType(@Nonnull PrimitiveType elementType) {
      switch (elementType.sort) {
         case Sort.BOOLEAN: return ArrayElementType.BOOLEAN;
         case Sort.CHAR:    return ArrayElementType.CHAR;
         case Sort.BYTE:    return ArrayElementType.BYTE;
         case Sort.SHORT:   return ArrayElementType.SHORT;
         case Sort.INT:     return ArrayElementType.INT;
         case Sort.FLOAT:   return ArrayElementType.FLOAT;
         case Sort.LONG:    return ArrayElementType.LONG;
         default:           return ArrayElementType.DOUBLE;
      }
   }

   /**
    * Returns the {@link Sort} of this Java type.
    */
   public int getSort() { return sort; }

   char getTypeCode() { return desc; }
   @Nonnull public Class<?> getType() { return type; }
   @Nullable public String getWrapperTypeDesc() { return wrapperTypeDesc; }

   @Override
   void getDescriptor(@Nonnull StringBuilder buf) {
      buf.append(desc);
   }

   @Nonnull @Override
   public String getClassName() {
      return type.getName();
   }

   @Nonnegative @Override
   public int getSize() { return size; }

   @Override
   public int getOpcode(int opcode) {
      int offset = opcode == IALOAD || opcode == IASTORE ? otherOffset : loadOrStoreOffset;
      return opcode + offset;
   }

   @Override
   public int getLoadOpcode() { return loadOpcode; }

   @Override
   public int getConstOpcode() { return constOpcode; }

   @Override
   public boolean equals(Object o) {
      return this == o || o instanceof PrimitiveType && sort == ((PrimitiveType) o).sort;
   }

   @Override
   public int hashCode() { return 13 * sort; }
}

package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.Opcodes.*;

public final class PrimitiveType extends JavaType
{
   private static final Class<?>[] TYPES = {
      void.class, boolean.class, char.class, byte.class, short.class, int.class, float.class, long.class, double.class
   };
   private static final String[] WRAPPER_TYPE_DESCS = {"java/lang/Void",
      "java/lang/Boolean", "java/lang/Character", "java/lang/Byte", "java/lang/Short",
      "java/lang/Integer", "java/lang/Float",     "java/lang/Long", "java/lang/Double"
   };
   private static final char[] TYPE_CODES = {'V', 'Z', 'C', 'B', 'S', 'I', 'F', 'J', 'D'};
   private static final String TYPE_CODES_STR = "VZCBSIFJD";
   private static final int[] ARRAY_ELEMENT_TYPES = {0,
      ArrayElementType.BOOLEAN, ArrayElementType.CHAR,  ArrayElementType.BYTE, ArrayElementType.SHORT,
      ArrayElementType.INT,     ArrayElementType.FLOAT, ArrayElementType.LONG, ArrayElementType.DOUBLE
   };
   private static final PrimitiveType VOID    = new PrimitiveType(0, 0);
   private static final PrimitiveType BOOLEAN = new PrimitiveType(1, 5);
   private static final PrimitiveType CHAR    = new PrimitiveType(2, 6);
   private static final PrimitiveType BYTE    = new PrimitiveType(3, 5);
   private static final PrimitiveType SHORT   = new PrimitiveType(4, 7);
   private static final PrimitiveType INT     = new PrimitiveType(5, 0);
   private static final PrimitiveType FLOAT   = new PrimitiveType(6, 2);
   private static final PrimitiveType LONG    = new PrimitiveType(7, 1);
   private static final PrimitiveType DOUBLE  = new PrimitiveType(8, 3);
   private static final PrimitiveType[] JAVA_TYPES = {VOID, BOOLEAN, CHAR, BYTE, SHORT, INT, FLOAT, LONG, DOUBLE};

   @Nonnegative private final int index;             // index of the type for lookup in several data type arrays
   @Nonnegative private final int loadOrStoreOffset; // instruction offset for IALOAD or IASTORE
   @Nonnegative private final int otherOffset;       // offset for all other instructions
   @Nonnegative private final int size;              // the size in words of the primitive type
   @Nonnegative private final int loadOpcode;        // the xLOAD instruction for this primitive type
   @Nonnegative private final int constOpcode;       // the xCONST_0 value for this primitive type
   @Nonnull private final String wrapperTypeDesc;    // internal name of the corresponding "java.lang" wrapper class

   private PrimitiveType(@Nonnegative int index, @Nonnegative int otherOffset) {
      super(1);
      this.index = index;
      this.otherOffset = otherOffset;
      wrapperTypeDesc = WRAPPER_TYPE_DESCS[index];

      Class<?> type = TYPES[index];

      if (type == void.class) {
         loadOrStoreOffset = 5;
         size = 0;
         loadOpcode = 0;
         constOpcode = 0;
      }
      else if (type == float.class) {
         loadOrStoreOffset = 2;
         size = 1;
         loadOpcode = FLOAD;
         constOpcode = FCONST_0;
      }
      else if (type == long.class) {
         loadOrStoreOffset = 1;
         size = 2;
         loadOpcode = LLOAD;
         constOpcode = LCONST_0;
      }
      else if (type == double.class) {
         loadOrStoreOffset = 3;
         size = 2;
         loadOpcode = DLOAD;
         constOpcode = DCONST_0;
      }
      else {
         loadOrStoreOffset = 0;
         size = 1;
         loadOpcode = ILOAD;
         constOpcode = ICONST_0;
      }
   }

   @Nonnull
   static PrimitiveType getPrimitiveType(@Nonnull Class<?> aClass) {
      for (int i = 0; i < 9; i++) {
         if (aClass == TYPES[i]) {
            return JAVA_TYPES[i];
         }
      }

      throw new IllegalArgumentException("Not a primitive type: " + aClass);
   }

   @Nullable
   static PrimitiveType getPrimitiveType(int typeCode) {
      int i = TYPE_CODES_STR.indexOf(typeCode);
      return i < 0 ? null : JAVA_TYPES[i];
   }

   @Nullable
   public static PrimitiveType getCorrespondingPrimitiveTypeIfWrapperType(@Nonnull String typeDesc) {
      for (int i = 0; i < 9; i++) {
         if (typeDesc.equals(WRAPPER_TYPE_DESCS[i])) {
            return JAVA_TYPES[i];
         }
      }

      return null;
   }

   @Nonnull
   public static Class<?> getType(int typeCode) {
      int i = TYPE_CODES_STR.indexOf((char) typeCode);
      return TYPES[i];
   }

   /**
    * Maps a <tt>PrimitiveType</tt> to the corresponding {@link ArrayElementType}.
    */
   public static int getArrayElementType(@Nonnull PrimitiveType elementType) {
      return ARRAY_ELEMENT_TYPES[elementType.index];
   }

   public char getTypeCode() { return TYPE_CODES[index]; }
   @Nonnull public Class<?> getType() { return TYPES[index]; }
   @Nonnull public String getWrapperTypeDesc() { return wrapperTypeDesc; }

   @Override
   void getDescriptor(@Nonnull StringBuilder buf) {
      buf.append(getTypeCode());
   }

   @Nonnull @Override
   public String getClassName() {
      return getType().getName();
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
      return this == o || o instanceof PrimitiveType && index == ((PrimitiveType) o).index;
   }

   @Override
   public int hashCode() { return 13 * index; }
}

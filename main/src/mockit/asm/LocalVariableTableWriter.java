package mockit.asm;

import javax.annotation.*;

import mockit.asm.constantPool.*;

/**
 * Writes the bytecode for the "LocalVariableTable" and "LocalVariableTypeTable" method code attributes.
 */
final class LocalVariableTableWriter extends AttributeWriter
{
   /**
    * Number of entries in the LocalVariableTable attribute.
    */
   @Nonnegative private int localVarCount;

   /**
    * The LocalVariableTable attribute.
    */
   @Nullable private ByteVector localVarTable;

   @Nonnegative private int localVarTypeAttributeIndex;

   /**
    * Number of entries in the LocalVariableTypeTable attribute.
    */
   @Nonnegative private int localVarTypeCount;

   /**
    * The LocalVariableTypeTable attribute.
    */
   @Nullable private ByteVector localVarTypeTable;

   LocalVariableTableWriter(@Nonnull ConstantPoolGeneration cp) { super(cp); }

   @Nonnegative
   int addLocalVariable(
      @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nonnull Label start, @Nonnull Label end,
      @Nonnegative int index
   ) {
      if (signature != null) {
         if (localVarTypeTable == null) {
            localVarTypeAttributeIndex = cp.newUTF8("LocalVariableTypeTable");
            localVarTypeTable = new ByteVector();
         }

         addAttribute(localVarTypeTable, name, signature, start, end, index);
         localVarTypeCount++;
      }

      if (localVarTable == null) {
         setAttribute("LocalVariableTable");
         localVarTable = new ByteVector();
      }

      addAttribute(localVarTable, name, desc, start, end, index);
      localVarCount++;

      char c = desc.charAt(0);
      int n = index + (c == 'J' || c == 'D' ? 2 : 1);
      return n;
   }

   private void addAttribute(
      @Nonnull ByteVector attribute, @Nonnull String name, @Nonnull String desc, @Nonnull Label start, @Nonnull Label end,
      @Nonnegative int index
   ) {
      attribute
         .putShort(start.position)
         .putShort(end.position - start.position)
         .putShort(cp.newUTF8(name)).putShort(cp.newUTF8(desc))
         .putShort(index);
   }

   @Nonnegative @Override
   protected int getSize() {
      return getSize(localVarTable) + getSize(localVarTypeTable);
   }

   @Nonnegative
   private static int getSize(@Nullable ByteVector attribute) { return attribute == null ? 0 : 8 + attribute.length; }

   @Nonnegative
   int getAttributeCount() { return (localVarTable == null ? 0 : 1) + (localVarTypeTable == null ? 0 : 1); }

   @Override
   protected void put(@Nonnull ByteVector out) {
      put(out, localVarTable, localVarCount);
      attributeIndex = localVarTypeAttributeIndex;
      put(out, localVarTypeTable, localVarTypeCount);
   }

   private void put(@Nonnull ByteVector out, @Nullable ByteVector attribute, @Nonnegative int numEntries) {
      if (attribute != null) {
         put(out, 2 + attribute.length);
         out.putShort(numEntries);
         out.putByteVector(attribute);
      }
   }
}

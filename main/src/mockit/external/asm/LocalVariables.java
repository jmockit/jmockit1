package mockit.external.asm;

import javax.annotation.*;

final class LocalVariables
{
   @Nonnull private final ConstantPoolGeneration cp;

   /**
    * Number of entries in the LocalVariableTable attribute.
    */
   @Nonnegative private int localVarCount;

   /**
    * The LocalVariableTable attribute.
    */
   @Nullable private ByteVector localVarTable;

   /**
    * Number of entries in the LocalVariableTypeTable attribute.
    */
   @Nonnegative private int localVarTypeCount;

   /**
    * The LocalVariableTypeTable attribute.
    */
   @Nullable private ByteVector localVarTypeTable;

   LocalVariables(@Nonnull ConstantPoolGeneration cp) { this.cp = cp; }

   @Nonnegative
   int addLocalVariable(
      @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nonnull Label start, @Nonnull Label end,
      @Nonnegative int index
   ) {
      if (signature != null) {
         localVarTypeTable = addAttribute(localVarTypeTable, name, signature, start, end, index);
         localVarTypeCount++;
      }

      localVarTable = addAttribute(localVarTable, name, desc, start, end, index);
      localVarCount++;

      char c = desc.charAt(0);
      int n = index + (c == 'J' || c == 'D' ? 2 : 1);
      return n;
   }

   @Nonnull
   private ByteVector addAttribute(
      @Nullable ByteVector attribute, @Nonnull String name, @Nonnull String desc,
      @Nonnull Label start, @Nonnull Label end, @Nonnegative int index
   ) {
      if (attribute == null) {
         attribute = new ByteVector();
      }

      attribute
         .putShort(start.position)
         .putShort(end.position - start.position)
         .putShort(cp.newUTF8(name)).putShort(cp.newUTF8(desc))
         .putShort(index);

      return attribute;
   }

   @Nonnegative
   int getSizeWhileAddingConstantPoolItems() {
      addItemToConstantPool("LocalVariableTable", localVarTable);
      addItemToConstantPool("LocalVariableTypeTable", localVarTypeTable);
      return getSize();
   }

   private void addItemToConstantPool(@Nonnull String attributeName, @Nullable ByteVector attribute) {
      if (attribute != null) {
         cp.newUTF8(attributeName);
      }
   }

   @Nonnegative
   int getSize() {
      return getSize(localVarTable) + getSize(localVarTypeTable);
   }

   @Nonnegative
   private static int getSize(@Nullable ByteVector attribute) { return attribute == null ? 0 : 8 + attribute.length; }

   @Nonnegative
   int getAttributeCount() { return (localVarTable == null ? 0 : 1) + (localVarTypeTable == null ? 0 : 1); }

   void put(@Nonnull ByteVector out) {
      put(out, "LocalVariableTable", localVarTable, localVarCount);
      put(out, "LocalVariableTypeTable", localVarTypeTable, localVarTypeCount);
   }

   private void put(
      @Nonnull ByteVector out, @Nonnull String attributeName, @Nullable ByteVector attribute,
      @Nonnegative int numEntries
   ) {
      if (attribute != null) {
         out.putShort(cp.newUTF8(attributeName));
         out.putInt(attribute.length + 2).putShort(numEntries);
         out.putByteVector(attribute);
      }
   }
}

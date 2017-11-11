package mockit.external.asm;

final class LocalVariables
{
   private final ClassWriter cw;

   /**
    * Number of entries in the LocalVariableTable attribute.
    */
   private int localVarCount;

   /**
    * The LocalVariableTable attribute.
    */
   private ByteVector localVar;

   /**
    * Number of entries in the LocalVariableTypeTable attribute.
    */
   private int localVarTypeCount;

   /**
    * The LocalVariableTypeTable attribute.
    */
   private ByteVector localVarType;

   LocalVariables(ClassWriter cw) { this.cw = cw; }

   int addLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
      if (signature != null) {
         localVarType = addAttribute(localVarType, name, signature, start, end, index);
         localVarTypeCount++;
      }

      localVar = addAttribute(localVar, name, desc, start, end, index);
      localVarCount++;

      char c = desc.charAt(0);
      int n = index + (c == 'J' || c == 'D' ? 2 : 1);
      return n;
   }

   private ByteVector addAttribute(ByteVector attribute, String name, String desc, Label start, Label end, int index) {
      if (attribute == null) {
         attribute = new ByteVector();
      }

      attribute.putShort(start.position)
         .putShort(end.position - start.position)
         .putShort(cw.newUTF8(name)).putShort(cw.newUTF8(desc))
         .putShort(index);

      return attribute;
   }

   int getSizeWhileAddingConstantPoolItems() {
      addItemToConstantPool("LocalVariableTable", localVar);
      addItemToConstantPool("LocalVariableTypeTable", localVarType);
      return getSize();
   }

   private void addItemToConstantPool(String attributeName, ByteVector attribute) {
      if (attribute != null) {
         cw.newUTF8(attributeName);
      }
   }

   int getSize() {
      return getSize(localVar) + getSize(localVarType);
   }

   private int getSize(ByteVector attribute) { return attribute == null ? 0 : 8 + attribute.length; }

   int getAttributeCount() { return (localVar == null ? 0 : 1) + (localVarType == null ? 0 : 1); }

   void put(ByteVector out) {
      put(out, "LocalVariableTable", localVar, localVarCount);
      put(out, "LocalVariableTypeTable", localVarType, localVarTypeCount);
   }

   private void put(ByteVector out, String attributeName, ByteVector attribute, int numEntries) {
      if (attribute != null) {
         out.putShort(cw.newUTF8(attributeName));
         out.putInt(attribute.length + 2).putShort(numEntries);
         out.putByteVector(attribute);
      }
   }
}

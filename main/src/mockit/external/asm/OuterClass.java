package mockit.external.asm;

final class OuterClass
{
   /**
    * The constant pool item that contains the name of the attribute to be produced.
    */
   private final int attributeName;

   /**
    * The constant pool item that contains the name of the enclosing class of this class.
    */
   private final int enclosingMethodOwner;

   /**
    * The constant pool item that contains the name and descriptor of the enclosing method of this class.
    */
   private final int enclosingMethod;

   OuterClass(ConstantPoolGeneration cp, String owner, String name, String desc) {
      attributeName = cp.newUTF8("EnclosingMethod");
      enclosingMethodOwner = cp.newClass(owner);
      enclosingMethod = name != null && desc != null ? cp.newNameType(name, desc) : 0;
   }

   int getSize() { return 10; }

   void put(ByteVector out) {
      out.putShort(attributeName).putInt(4);
      out.putShort(enclosingMethodOwner).putShort(enclosingMethod);
   }
}

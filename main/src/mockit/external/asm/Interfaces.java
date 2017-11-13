package mockit.external.asm;

final class Interfaces
{
   /**
    * Number of interfaces implemented or extended by this class or interface.
    */
   private final int interfaceCount;

   /**
    * The interfaces implemented or extended by this class or interface. More precisely, this array contains the indexes
    * of the constant pool items that contain the internal names of these interfaces.
    */
   private final int[] interfaces;

   Interfaces(ClassWriter cw, String[] interfaces) {
      int n = interfaceCount = interfaces.length;
      this.interfaces = new int[n];

      for (int i = 0; i < n; ++i) {
         this.interfaces[i] = cw.newClass(interfaces[i]);
      }
   }

   int getCount() { return interfaceCount; }

   void put(ByteVector out) {
      for (int i = 0; i < interfaceCount; i++) {
         out.putShort(interfaces[i]);
      }
   }
}

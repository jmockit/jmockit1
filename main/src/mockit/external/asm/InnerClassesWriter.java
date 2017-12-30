package mockit.external.asm;

import javax.annotation.*;

final class InnerClassesWriter extends AttributeWriter
{
   /**
    * The <tt>InnerClasses</tt> attribute.
    */
   @Nonnull private final ByteVector innerClasses;

   /**
    * The number of entries in the <tt>InnerClasses</tt> attribute.
    */
   @Nonnegative private int innerClassesCount;

   InnerClassesWriter(@Nonnull ConstantPoolGeneration cp) {
      super(cp, "InnerClasses");
      innerClasses = new ByteVector();
   }

   void add(@Nonnull String name, @Nullable String outerName, @Nullable String innerName, int access) {
      StringItem nameItem = cp.newClassItem(name);

      // Sec. 4.7.6 of the JVMS states "Every CONSTANT_Class_info entry in the constant_pool table which represents a
      // class or interface C that is not a package member must have exactly one corresponding entry in the classes
      // array". To avoid duplicates we keep track in the Item of each CONSTANT_Class_info entry C whether an inner
      // class entry has already been added for C. If so we store the index of this inner class entry (plus one).
      if (nameItem.innerClassIndex == 0) {
         add(nameItem, outerName, innerName, access);
         nameItem.innerClassIndex = ++innerClassesCount;
      }
   }

   private void add(@Nonnull StringItem nameItem, @Nullable String outerName, @Nullable String innerName, int access) {
      innerClasses.putShort(nameItem.index);
      innerClasses.putShort(outerName == null ? 0 : cp.newClass(outerName));
      innerClasses.putShort(innerName == null ? 0 : cp.newUTF8(innerName));
      innerClasses.putShort(access);
   }

   @Nonnegative @Override
   int getSize() { return 8 + innerClasses.length; }

   @Override
   void put(@Nonnull ByteVector out) {
      put(out, innerClasses.length + 2);
      out.putShort(innerClassesCount);
      out.putByteVector(innerClasses);
   }
}

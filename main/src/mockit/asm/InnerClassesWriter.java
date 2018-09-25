package mockit.asm;

import javax.annotation.*;

final class InnerClassesWriter extends AttributeWriter
{
   @Nonnull private final ByteVector innerClasses;
   @Nonnegative private int innerClassesCount;

   InnerClassesWriter(@Nonnull ConstantPoolGeneration cp) {
      super(cp, "InnerClasses");
      innerClasses = new ByteVector();
   }

   void add(@Nonnull String name, @Nullable String outerName, @Nullable String innerName, int access) {
      innerClasses.putShort(cp.newClass(name));
      innerClasses.putShort(outerName == null ? 0 : cp.newClass(outerName));
      innerClasses.putShort(innerName == null ? 0 : cp.newUTF8(innerName));
      innerClasses.putShort(access);
      innerClassesCount++;
   }

   @Nonnegative @Override
   int getSize() { return 8 + innerClasses.length; }

   @Override
   void put(@Nonnull ByteVector out) {
      put(out, 2 + innerClasses.length);
      out.putShort(innerClassesCount);
      out.putByteVector(innerClasses);
   }
}

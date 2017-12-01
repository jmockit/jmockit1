package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.Opcodes.*;

public abstract class ReferenceType extends JavaType
{
   /**
    * A buffer containing the internal name of this Java type. This field is only used for reference types.
    */
   @Nonnull final char[] buf;

   /**
    * The offset of the internal name of this Java type in {@link #buf}.
    */
   @Nonnegative final int off;

   ReferenceType(@Nonnull char[] buf, @Nonnegative int off, @Nonnegative int len) {
      super(len);
      this.buf = buf;
      this.off = off;
   }

   /**
    * Returns the object or array type corresponding to the given internal name.
    */
   @Nonnull
   public static ReferenceType getObjectType(@Nonnull String internalName) {
      char[] buf = internalName.toCharArray();
      return buf[0] == '[' ? new ArrayType(buf, 0, buf.length) : new ObjectType(buf, 0, buf.length);
   }

   static void getDescriptor(@Nonnull StringBuilder buf, @Nonnull Class<?> aClass) {
      buf.append('L');

      String name = aClass.getName();
      int len = name.length();

      for (int i = 0; i < len; i++) {
         char c = name.charAt(i);
         buf.append(c == '.' ? '/' : c);
      }

      buf.append(';');
   }

   @Override
   void getDescriptor(@Nonnull StringBuilder buf) {
      buf.append(this.buf, off, len);
   }

   /**
    * Returns the internal name of the class corresponding to this object or array type. The internal name of a class is
    * its fully qualified name (as returned by Class.getName(), where '.' are replaced by '/'.
    *
    * @return the internal name of the class corresponding to this object type.
    */
   @Nonnull
   public final String getInternalName() {
      return new String(buf, off, len);
   }

   @Override public int getSize() { return 1; }

   @Override public int getOpcode(int opcode) { return opcode + 4; }

   @Override
   public final int getLoadOpcode() { return ALOAD; }

   @Override
   public final int getConstOpcode() { return ACONST_NULL; }

   @Override
   public final boolean equals(Object o) {
      if (this == o) {
         return true;
      }

      if (!(o instanceof ReferenceType)) {
         return false;
      }

      ReferenceType t = (ReferenceType) o;

      if (getClass() != t.getClass() || len != t.len) {
         return false;
      }

      for (int i = off, j = t.off, end = i + len; i < end; i++, j++) {
         if (buf[i] != t.buf[j]) {
            return false;
         }
      }

      return true;
   }

   @Override
   public final int hashCode() {
      int hc = 13;

      for (int i = off, end = i + len; i < end; i++) {
         hc = 17 * (hc + buf[i]);
      }

      return hc;
   }
}

package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.Opcodes.*;

public abstract class ReferenceType extends JavaType
{
   /**
    * A buffer containing the internal name of this Java reference type.
    */
   @Nonnull final char[] buf;

   /**
    * The offset of the internal name of this Java type in {@link #buf}.
    */
   @Nonnegative final int off;

   ReferenceType(@Nonnull char[] buf) {
      super(buf.length);
      this.buf = buf;
      off = 0;
   }

   ReferenceType(@Nonnull char[] buf, @Nonnegative int off, @Nonnegative int len) {
      super(len);
      this.buf = buf;
      this.off = off;
   }

   /**
    * Returns the Java type corresponding to the given type descriptor.
    *
    * @param typeDesc a type descriptor.
    */
   @Nonnull
   public static ReferenceType createFromTypeDescriptor(@Nonnull String typeDesc) {
      return getReferenceType(typeDesc.toCharArray(), 0);
   }

   /**
    * Returns the Java type corresponding to the given type descriptor. For method descriptors, <tt>buf</tt> is supposed
    * to contain nothing more than the descriptor itself.
    *
    * @param buf a buffer containing a type descriptor.
    * @param off the offset of this descriptor in the previous buffer.
    */
   @Nonnull
   static ReferenceType getReferenceType(@Nonnull char[] buf, @Nonnegative int off) {
      switch (buf[off]) {
         case '[': return ArrayType.create(buf, off);
         case 'L': return ObjectType.create(buf, off);
         case '(': return new MethodType(buf, off, buf.length - off);
         default:  throw new IllegalArgumentException("Invalid type descriptor: " + new String(buf));
      }
   }

   /**
    * Returns the object or array type corresponding to the given internal name.
    */
   @Nonnull
   public static ReferenceType createFromInternalName(@Nonnull String internalName) {
      char[] buf = internalName.toCharArray();
      return buf[0] == '[' ? new ArrayType(buf) : new ObjectType(buf);
   }

   static int findTypeNameLength(@Nonnull char[] buf, @Nonnegative int off, @Nonnegative int len) {
      len++;

      while (buf[off + len] != ';') {
         len++;
      }

      return len;
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
    * its fully qualified name (as returned by Class.getName(), where '.' are replaced by '/'. For an array type, it
    * starts with "[" and ends with the type descriptor of the array element type.
    *
    * @return the internal name of the class corresponding to this object or array type.
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

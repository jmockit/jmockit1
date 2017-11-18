package mockit.external.asm;

import javax.annotation.*;

abstract class ReferenceType extends Type
{
   /**
    * A buffer containing the internal name of this Java type. This field is only used for reference types.
    */
   final char[] buf;

   /**
    * The offset of the internal name of this Java type in {@link #buf}.
    */
   final int off;

   ReferenceType(int sort, char[] buf, int off, int len) {
      super(sort, len);
      this.buf = buf;
      this.off = off;
   }

   static void getDescriptor(@Nonnull StringBuffer buf, @Nonnull Class<?> aClass) {
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
   void getDescriptor(@Nonnull StringBuffer buf) {
      buf.append(this.buf, off, len);
   }

   @Nonnull @Override
   public final String getInternalName() {
      return new String(buf, off, len);
   }

   @Override public int getSize() { return 1; }
   @Override public int getOpcode(int opcode) { return opcode + 4; }

   @Override
   public final boolean equals(Object o) {
      if (this == o) {
         return true;
      }

      if (!(o instanceof ReferenceType)) {
         return false;
      }

      ReferenceType t = (ReferenceType) o;

      if (sort != t.sort || len != t.len) {
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
      int hc = 13 * sort;

      for (int i = off, end = i + len; i < end; i++) {
         hc = 17 * (hc + buf[i]);
      }

      return hc;
   }
}

package mockit.asm.exceptionHandling;

import javax.annotation.*;

import mockit.asm.*;

/**
 * Information about an exception handler block.
 */
final class ExceptionHandler
{
   /**
    * Beginning of the exception handler's scope (inclusive).
    */
   @Nonnull Label start;

   /**
    * End of the exception handler's scope (exclusive).
    */
   @Nonnull Label end;

   /**
    * Beginning of the exception handler's code.
    */
   @Nonnull final Label handler;

   /**
    * Internal name of the type of exceptions handled by this handler, or <tt>null</tt> to catch any exceptions.
    */
   @Nullable private final String desc;

   /**
    * Constant pool index of the internal name of the type of exceptions handled by this handler, or <tt>0</tt> to catch
    * any exceptions.
    */
   @Nonnegative private final int type;

   /**
    * Next exception handler block info.
    */
   @Nullable ExceptionHandler next;

   ExceptionHandler(@Nonnull Label start, @Nonnull Label end, @Nonnull Label handler, @Nullable String desc, @Nonnegative int type) {
      this.start = start;
      this.end = end;
      this.handler = handler;
      this.desc = desc;
      this.type = type;
   }

   @Nonnull
   String getCatchTypeDesc() { return desc == null ? "java/lang/Throwable" : desc; }

   /**
    * Removes the range between start and end from the given exception handlers.
    *
    * @param h     an exception handler list.
    * @param start the start of the range to be removed.
    * @param end   the end of the range to be removed. Maybe null.
    * @return the exception handler list with the start-end range removed.
    */
   @Nullable
   static ExceptionHandler remove(@Nullable ExceptionHandler h, @Nonnull Label start, @Nullable Label end) {
      if (h == null) {
         return null;
      }

      h.next = remove(h.next, start, end);
      int hStart = h.start.position;
      int hEnd = h.end.position;
      int s = start.position;
      int e = end == null ? Integer.MAX_VALUE : end.position;

      // If [hStart,hEnd[ and [s,e[ intervals intersect...
      if (s < hEnd && e > hStart) {
         if (s <= hStart) {
            if (e >= hEnd) {
               // [hStart,hEnd[ fully included in [s,e[, h removed
               h = h.next;
            }
            else {
               // [hStart,hEnd[ minus [s,e[ = [e,hEnd[
               h.start = end;
            }
         }
         else if (e >= hEnd) {
            // [hStart,hEnd[ minus [s,e[ = [hStart,s[
            h.end = start;
         }
         else {
            // [hStart,hEnd[ minus [s,e[ = [hStart,s[ + [e,hEnd[
            ExceptionHandler g = new ExceptionHandler(end, h.end, h.handler, h.desc, h.type);
            g.next = h.next;
            h.end = start;
            h.next = g;
         }
      }

      return h;
   }

   void put(@Nonnull ByteVector out) {
      out.putShort(start.position).putShort(end.position);
      out.putShort(handler.position).putShort(type);
   }
}

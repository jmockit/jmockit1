/*
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package mockit.external.asm;

import javax.annotation.*;

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

   ExceptionHandler(
      @Nonnull Label start, @Nonnull Label end, @Nonnull Label handler, @Nullable String desc, @Nonnegative int type
   ) {
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
               //noinspection ConstantConditions
               h.start = end;
            }
         }
         else if (e >= hEnd) {
            // [hStart,hEnd[ minus [s,e[ = [hStart,s[
            h.end = start;
         }
         else {
            // [hStart,hEnd[ minus [s,e[ = [hStart,s[ + [e,hEnd[
            //noinspection ConstantConditions
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

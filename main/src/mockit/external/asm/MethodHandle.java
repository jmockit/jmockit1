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
 * A reference to a method.
 */
final class MethodHandle
{
   interface Tag
   {
//    int INVOKEVIRTUAL    = 5;
//    int INVOKESTATIC     = 6;
//    int INVOKESPECIAL    = 7;
//    int NEWINVOKESPECIAL = 8;
      int INVOKEINTERFACE  = 9;
   }

   /**
    * The kind of method designated by this handle. Should be one of the {@link Tag} constants.
    */
   @Nonnegative final int tag;

   /**
    * The internal name of the class that owns the method designated by this handle.
    */
   @Nonnull final String owner;

   /**
    * The name of the method designated by this handle.
    */
   @Nonnull final String name;

   /**
    * The descriptor of the method designated by this handle.
    */
   @Nonnull final String desc;

   /**
    * Initializes a new method handle.
    *
    * @param tag   the kind of method designated by this handle. Must be one of the {@link Tag} constants.
    * @param owner the internal name of the class that owns the method designated by this handle.
    * @param name  the name of the method designated by this handle.
    * @param desc  the descriptor of the method designated by this handle.
    */
   MethodHandle(@Nonnegative int tag, @Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
      this.tag = tag;
      this.owner = owner;
      this.name = name;
      this.desc = desc;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }

      if (!(obj instanceof MethodHandle)) {
         return false;
      }

      MethodHandle h = (MethodHandle) obj;
      return tag == h.tag && owner.equals(h.owner) && name.equals(h.name) && desc.equals(h.desc);
   }

   @Override
   public int hashCode() {
      return tag + owner.hashCode() * name.hashCode() * desc.hashCode();
   }
}

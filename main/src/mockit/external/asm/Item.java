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
 * A constant pool item.
 */
abstract class Item
{
   /**
    * Constants for types of items in the constant pool of a class.
    */
   interface Type
   {
      int CLASS     =  7; // CONSTANT_Class
      int FIELD     =  9; // CONSTANT_Fieldref
      int METH      = 10; // CONSTANT_Methodref
      int IMETH     = 11; // CONSTANT_InterfaceMethodref
      int STR       =  8; // CONSTANT_String
      int INT       =  3; // CONSTANT_Integer
      int FLOAT     =  4; // CONSTANT_Float
      int LONG      =  5; // CONSTANT_Long
      int DOUBLE    =  6; // CONSTANT_Double
      int NAME_TYPE = 12; // CONSTANT_NameAndType
      int UTF8      =  1; // CONSTANT_Utf8
      int MTYPE     = 16; // CONSTANT_MethodType
      int HANDLE    = 15; // CONSTANT_MethodHandle
      int INDY      = 18; // CONSTANT_InvokeDynamic

      /**
       * The base value for all CONSTANT_MethodHandle constant pool items.
       * Internally, ASM stores the 9 variations of CONSTANT_MethodHandle into 9 different items.
       */
      int HANDLE_BASE = 20;

      /**
       * The type of BootstrapMethod items. These items are stored in a special class attribute named "BootstrapMethods"
       * and not in the constant pool.
       */
      int BSM = 33;
   }

   /**
    * Index of this item in the constant pool.
    */
   @Nonnegative final int index;

   /**
    * Type of this constant pool item. The value of this field is one of the {@link Type} constants.
    * <p/>
    * MethodHandle constant 9 variations are stored using a range of 9 values from {@link Type#HANDLE_BASE} + 1 to
    * {@link Type#HANDLE_BASE} + 9.
    * <p/>
    * Special Item types are used for Items that are stored in the {@link ConstantPoolGeneration#typeTable}, instead of
    * the constant pool, in order to avoid clashes with normal constant pool items in the constant pool's hash table.
    * These special item types are defined in {@link TypeTableItem.SpecialType}.
    */
   int type;

   /**
    * The hash code value of this constant pool item.
    */
   int hashCode;

   /**
    * Link to another constant pool item, used for collision lists in the constant pool's hash table.
    */
   @Nullable Item next;

   /**
    * Initializes an Item for a constant pool element at the given position.
    *
    * @param index index of the item.
    */
   Item(@Nonnegative int index) { this.index = index; }

   /**
    * Initializes a copy of the given item.
    *
    * @param index index of the item to be constructed.
    * @param item  the item that must be copied into the item to be constructed.
    */
   Item(@Nonnegative int index, @Nonnull Item item) {
      this.index = index;
      type = item.type;
      hashCode = item.hashCode;
   }

   final void setHashCode(int valuesHashCode) {
      hashCode = 0x7FFFFFFF & (type + valuesHashCode);
   }

   final void setNext(@Nonnull Item[] items) {
      int index = hashCode % items.length;
      next = items[index];
      items[index] = this;
   }

   /**
    * Indicates if the given item is equal to this one. <i>This method assumes that the two items have the same
    * {@link #type}</i>.
    *
    * @param item the item to be compared to this one. Both items must have the same {@link #type}.
    * @return <tt>true</tt> if the given item if equal to this one, <tt>false</tt> otherwise.
    */
   abstract boolean isEqualTo(@Nonnull Item item);
}

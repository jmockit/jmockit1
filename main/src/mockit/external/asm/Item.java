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

import static mockit.external.asm.ConstantPoolItemType.*;

/**
 * A constant pool item.
 */
class Item
{
   /**
    * Defines constants for {@link #NORMAL normal}, {@link #UNINIT uninitialized}, and {@link #MERGED merged} special
    * item types stored in the {@link ConstantPoolGeneration#typeTable}, instead of the constant pool, in order to avoid
    * clashes with normal constant pool items in the ClassWriter constant pool's hash table.
    */
   interface SpecialType
   {
      int NORMAL = 30;
      int UNINIT = 31;
      int MERGED = 32;
   }

   /**
    * Index of this item in the constant pool.
    */
   @Nonnegative final int index;

   /**
    * Type of this constant pool item. A single class is used to represent all constant pool item types, in order to
    * minimize the bytecode size of this package. The value of this field is one of the {@link ConstantPoolItemType}
    * constants.
    * <p/>
    * MethodHandle constant 9 variations are stored using a range of 9 values from
    * {@link ConstantPoolItemType#HANDLE_BASE} + 1 to {@link ConstantPoolItemType#HANDLE_BASE} + 9.
    * <p/>
    * Special Item types are used for Items that are stored in the ClassWriter {@link ConstantPoolGeneration#typeTable},
    * instead of the constant pool, in order to avoid clashes with normal constant pool items in the ClassWriter
    * constant pool's hash table. These special item types are defined in {@link SpecialType}.
    */
   int type;

   /**
    * Value of this item, for an integer item.
    */
   int intVal;

   /**
    * Value of this item, for a long item.
    */
   long longVal;

   /**
    * First part of the value of this item, for items that do not hold a primitive value.
    */
   String strVal1;

   /**
    * Second part of the value of this item, for items that do not hold a primitive value.
    */
   @Nullable String strVal2;

   /**
    * Third part of the value of this item, for items that do not hold a primitive value.
    */
   @Nullable String strVal3;

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
      intVal = item.intVal;
      longVal = item.longVal;
      strVal1 = item.strVal1;
      strVal2 = item.strVal2;
      strVal3 = item.strVal3;
      hashCode = item.hashCode;
   }

   /**
    * Indicates if the given item is equal to this one. <i>This method assumes that the two items have the same
    * {@link #type}</i>.
    *
    * @param item the item to be compared to this one. Both items must have the same {@link #type}.
    * @return <tt>true</tt> if the given item if equal to this one, <tt>false</tt> otherwise.
    */
   final boolean isEqualTo(@Nonnull Item item) {
      switch (type) {
         case UTF8:
         case STR:
         case CLASS:
         case MTYPE:
         case SpecialType.NORMAL:
            return item.strVal1.equals(strVal1);
         case SpecialType.MERGED:
         case LONG:
         case DOUBLE:
            return item.longVal == longVal;
         case INT:
         case FLOAT:
            return item.intVal == intVal;
         case SpecialType.UNINIT:
            return item.intVal == intVal && item.strVal1.equals(strVal1);
         case NAME_TYPE:
            //noinspection ConstantConditions
            return item.strVal1.equals(strVal1) && item.strVal2.equals(strVal2);
         case INDY:
            //noinspection ConstantConditions
            return item.longVal == longVal && item.strVal1.equals(strVal1) && item.strVal2.equals(strVal2);
         // case FIELD|METH|IMETH|HANDLE_BASE + 1..9:
         default:
            //noinspection ConstantConditions
            return item.strVal1.equals(strVal1) && item.strVal2.equals(strVal2) && item.strVal3.equals(strVal3);
      }
   }

   final boolean isDoubleSized() {
      return type == LONG || type == DOUBLE;
   }

   /**
    * Recovers the stack size variation from this constant pool item, computing and storing it if needed.
    * In order not to recompute several times this variation for the same Item, we use the intVal field of this item to
    * store this variation, once it has been computed. More precisely this intVal field stores the sizes of the
    * arguments and of the return value corresponding to desc.
    */
   @Nonnegative
   final int getArgSizeComputingIfNeeded(@Nonnull String desc) {
      int argSize = intVal;

      if (argSize == 0) {
         argSize = JavaType.getArgumentsAndReturnSizes(desc);
         intVal = argSize;
      }

      return argSize;
   }

   final void setNext(@Nonnull Item[] items2) {
      int index2 = hashCode % items2.length;
      next = items2[index2];
      items2[index2] = this;
   }
}

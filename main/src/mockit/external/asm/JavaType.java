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

import java.lang.reflect.*;
import javax.annotation.*;

/**
 * A Java field or method type. This class can be used to make it easier to manipulate type and method descriptors.
 */
public abstract class JavaType
{
   /**
    * See {@link #getSort()}.
    */
   public interface Sort
   {
      /**
       * The sort of the <tt>void</tt> type.
       */
      int VOID = 0;

      /**
       * The sort of the <tt>boolean</tt> type.
       */
      int BOOLEAN = 1;

      /**
       * The sort of the <tt>char</tt> type.
       */
      int CHAR = 2;

      /**
       * The sort of the <tt>byte</tt> type.
       */
      int BYTE = 3;

      /**
       * The sort of the <tt>short</tt> type.
       */
      int SHORT = 4;

      /**
       * The sort of the <tt>int</tt> type.
       */
      int INT = 5;

      /**
       * The sort of the <tt>float</tt> type.
       */
      int FLOAT = 6;

      /**
       * The sort of the <tt>long</tt> type.
       */
      int LONG = 7;

      /**
       * The sort of the <tt>double</tt> type.
       */
      int DOUBLE = 8;

      /**
       * The sort of array reference types.
       */
      int ARRAY = 9;

      /**
       * The sort of object reference types.
       */
      int OBJECT = 10;

      /**
       * The sort of method types.
       */
      int METHOD = 11;
   }

   /**
    * The sort of this Java type.
    */
   final int sort;

   /**
    * The length of the internal name of this Java type.
    */
   @Nonnegative final int len;

   // ------------------------------------------------------------------------
   // Constructors and static factory methods
   // ------------------------------------------------------------------------

   /**
    * Constructs a Java type.
    *
    * @param sort the sort of the reference type to be constructed.
    * @param len  the length of this descriptor.
    */
   JavaType(int sort, @Nonnegative int len) {
      this.sort = sort;
      this.len = len;
   }

   /**
    * Returns the Java type corresponding to the given type descriptor.
    *
    * @param typeDescriptor a field or method type descriptor.
    */
   @Nonnull
   public static JavaType getType(@Nonnull String typeDescriptor) {
      return getType(typeDescriptor.toCharArray(), 0);
   }

   /**
    * Returns the Java type corresponding to the given class.
    */
   @Nonnull
   public static JavaType getType(@Nonnull Class<?> aClass) {
      if (aClass.isPrimitive()) {
         return PrimitiveType.getPrimitiveType(aClass);
      }

      String typeDesc = getDescriptor(aClass);
      return getType(typeDesc);
   }

   /**
    * Returns the Java method type corresponding to the given constructor.
    */
   @Nonnull
   public static JavaType getType(@Nonnull Constructor<?> constructor) {
      String constructorDesc = getConstructorDescriptor(constructor);
      return getType(constructorDesc);
   }

   /**
    * Returns the Java method type corresponding to the given method.
    */
   @Nonnull
   public static JavaType getType(@Nonnull Method method) {
      String methodDesc = getMethodDescriptor(method);
      return getType(methodDesc);
   }

   /**
    * Returns the Java types corresponding to the argument types of the given method descriptor.
    */
   @Nonnull
   public static JavaType[] getArgumentTypes(@Nonnull String methodDescriptor) {
      char[] buf = methodDescriptor.toCharArray();
      int off = 1;
      int size = 0;

      while (true) {
         char c = buf[off++];

         if (c == ')') {
            break;
         }
         else if (c == 'L') {
            while (buf[off++] != ';') {}
            size++;
         }
         else if (c != '[') {
            size++;
         }
      }

      JavaType[] argTypes = new JavaType[size];
      off = 1;
      size = 0;

      while (buf[off] != ')') {
         JavaType argType = getType(buf, off);
         argTypes[size] = argType;
         off += argType.len + (argType.sort == Sort.OBJECT ? 2 : 0);
         size++;
      }

      return argTypes;
   }

   /**
    * Returns the Java type corresponding to the return type of the given method descriptor.
    */
   @Nonnull
   public static JavaType getReturnType(@Nonnull String methodDescriptor) {
      char[] buf = methodDescriptor.toCharArray();
      return getType(buf, methodDescriptor.indexOf(')') + 1);
   }

   /**
    * Computes the size of the arguments and of the return value of a method.
    *
    * @param desc the descriptor of a method.
    * @return the size of the arguments of the method (plus one for the implicit <tt>this</tt> argument),
    * <tt>argSize</tt>, and the size of its return value, <tt>retSize</tt>, packed into a single
    * <tt>int i = (argSize << 2) | retSize</tt> (<tt>argSize</tt> is therefore equal to <tt>i >> 2</tt>, and
    * <tt>retSize</tt> to <tt>i & 0x03</tt>).
    */
   public static int getArgumentsAndReturnSizes(@Nonnull String desc) {
      int argSize = 1;
      int i = 1;

      while (true) {
         char c = desc.charAt(i++);

         if (c == ')') {
            c = desc.charAt(i);
            return argSize << 2 | (c == 'V' ? 0 : c == 'D' || c == 'J' ? 2 : 1);
         }
         else if (c == 'L') {
            while (desc.charAt(i++) != ';') {}
            argSize++;
         }
         else if (c == '[') {
            while ((c = desc.charAt(i)) == '[') {
               i++;
            }

            if (c == 'D' || c == 'J') {
               argSize--;
            }
         }
         else if (c == 'D' || c == 'J') {
            argSize += 2;
         }
         else {
            argSize++;
         }
      }
   }

   /**
    * Returns the Java type corresponding to the given type descriptor. For method descriptors, <tt>buf</tt> is supposed
    * to contain nothing more than the descriptor itself.
    *
    * @param buf a buffer containing a type descriptor.
    * @param off the offset of this descriptor in the previous buffer.
    */
   @Nonnull
   static JavaType getType(@Nonnull char[] buf, @Nonnegative int off) {
      char typeCode = buf[off];
      JavaType type = PrimitiveType.getPrimitiveType(typeCode);

      if (type != null) {
         return type;
      }

      switch (typeCode) {
         case '[': return ArrayType.create(buf, off);
         case 'L': return ObjectType.create(buf, off);
         // case '(':
         default: return new MethodType(buf, off, buf.length - off);
      }
   }

   // ------------------------------------------------------------------------
   // Accessors
   // ------------------------------------------------------------------------

   /**
    * Returns the {@link Sort} of this Java type.
    */
   public int getSort() { return sort; }

   /**
    * Returns the binary name of the class corresponding to this type. This method must not be used on method types.
    */
   @Nonnull
   public abstract String getClassName();

   // ------------------------------------------------------------------------
   // Conversion to type descriptors
   // ------------------------------------------------------------------------

   /**
    * Returns the descriptor corresponding to this Java type.
    */
   @Nonnull
   public final String getDescriptor() {
      StringBuilder buf = new StringBuilder();
      getDescriptor(buf);
      return buf.toString();
   }

   /**
    * Appends the descriptor corresponding to this Java type to the given string buffer.
    *
    * @param buf the string builder to which the descriptor must be appended.
    */
   abstract void getDescriptor(@Nonnull StringBuilder buf);

   // -------------------------------------------------------------------------------------------------------
   // Direct conversion from classes to type descriptors, and vice-versa, without intermediate JavaType objects
   // -------------------------------------------------------------------------------------------------------

   /**
    * Returns the internal name of the given class. The internal name of a class is its fully qualified name, as
    * returned by Class.getName(), where '.' are replaced by '/'.
    *
    * @param aClass an object or array class.
    */
   @Nonnull
   public static String getInternalName(@Nonnull Class<?> aClass) {
      return aClass.getName().replace('.', '/');
   }

   /**
    * Returns the descriptor corresponding to the given Java type.
    *
    * @param aClass an object class, a primitive class or an array class.
    */
   @Nonnull
   public static String getDescriptor(@Nonnull Class<?> aClass) {
      StringBuilder buf = new StringBuilder();
      getDescriptor(buf, aClass);
      return buf.toString();
   }

   /**
    * Returns the descriptor corresponding to the given constructor.
    */
   @Nonnull
   public static String getConstructorDescriptor(@Nonnull Constructor<?> constructor) {
      Class<?>[] parameters = constructor.getParameterTypes();
      StringBuilder buf = new StringBuilder();
      buf.append('(');

      for (Class<?> parameter : parameters) {
         getDescriptor(buf, parameter);
      }

      return buf.append(")V").toString();
   }

   /**
    * Returns the descriptor corresponding to the given method.
    */
   @Nonnull
   public static String getMethodDescriptor(@Nonnull Method method) {
      Class<?>[] parameters = method.getParameterTypes();
      StringBuilder buf = new StringBuilder();
      buf.append('(');

      for (Class<?> parameter : parameters) {
         getDescriptor(buf, parameter);
      }

      buf.append(')');
      getDescriptor(buf, method.getReturnType());
      return buf.toString();
   }

   /**
    * Appends the descriptor of the given class to the given string builder.
    */
   private static void getDescriptor(@Nonnull StringBuilder buf, @Nonnull Class<?> aClass) {
      Class<?> d = aClass;

      while (true) {
         if (d.isPrimitive()) {
            char typeCode = PrimitiveType.getTypeCode(d);
            buf.append(typeCode);
            return;
         }
         else if (d.isArray()) {
            buf.append('[');
            d = d.getComponentType();
         }
         else {
            ReferenceType.getDescriptor(buf, d);
            return;
         }
      }
   }

   // ------------------------------------------------------------------------
   // Corresponding size and opcodes
   // ------------------------------------------------------------------------

   /**
    * Returns the size of values of this type. This method must not be used for method types.
    *
    * @return the size of values of this type, i.e., 2 for <tt>long</tt> and <tt>double</tt>, 0 for <tt>void</tt> and 1
    * otherwise.
    */
   public abstract int getSize();

   /**
    * Returns a JVM instruction opcode adapted to this Java type. This method must not be used for method types.
    *
    * @param opcode a JVM instruction opcode. This opcode must be one of ILOAD, ISTORE, IALOAD, IASTORE, IADD, ISUB,
    *               IMUL, IDIV, IREM, INEG, ISHL, ISHR, IUSHR, IAND, IOR, IXOR and IRETURN.
    * @return an opcode that is similar to the given opcode, but adapted to this Java type. For example, if this type is
    * <tt>float</tt> and <tt>opcode</tt> is IRETURN, this method returns FRETURN.
    */
   public abstract int getOpcode(int opcode);

   // ------------------------------------------------------------------------
   // Equals, hashCode and toString
   // ------------------------------------------------------------------------

   /**
    * Returns a string representation of this type.
    *
    * @return the descriptor of this type.
    */
   @Override
   public final String toString() {
      return getDescriptor();
   }
}

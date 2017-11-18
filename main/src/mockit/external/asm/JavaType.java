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
 *
 * @author Eric Bruneton
 * @author Chris Nokleberg
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
   final int len;

   /**
    * Maps a {@link Sort} to the corresponding {@link ArrayElementType}.
    */
   public static int getArrayElementType(int elementSort) {
      switch (elementSort) {
         case Sort.BOOLEAN: return ArrayElementType.BOOLEAN;
         case Sort.CHAR:    return ArrayElementType.CHAR;
         case Sort.BYTE:    return ArrayElementType.BYTE;
         case Sort.SHORT:   return ArrayElementType.SHORT;
         case Sort.INT:     return ArrayElementType.INT;
         case Sort.FLOAT:   return ArrayElementType.FLOAT;
         case Sort.LONG:    return ArrayElementType.LONG;
         default:           return ArrayElementType.DOUBLE;
      }
   }

   // ------------------------------------------------------------------------
   // Constructors and static factory methods
   // ------------------------------------------------------------------------

   /**
    * Constructs a Java type.
    *
    * @param sort the sort of the reference type to be constructed.
    * @param len  the length of this descriptor.
    */
   JavaType(int sort, int len) {
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
    * Returns the Java type corresponding to the given internal name.
    */
   @Nonnull
   public static JavaType getObjectType(@Nonnull String internalName) {
      char[] buf = internalName.toCharArray();
      return buf[0] == '[' ? new ArrayType(buf, 0, buf.length) : new ObjectType(buf, 0, buf.length);
   }

   /**
    * Returns the Java type corresponding to the given method descriptor.
    * Equivalent to <code>JavaType.getType(methodDescriptor)</code>.
    */
   @Nonnull
   public static JavaType getMethodType(@Nonnull String methodDescriptor) {
      return getType(methodDescriptor.toCharArray(), 0);
   }

   /**
    * Returns the Java type corresponding to the given class.
    */
   @Nonnull
   public static JavaType getType(@Nonnull Class<?> c) {
      if (c.isPrimitive()) {
         return PrimitiveType.getPrimitiveType(c);
      }

      String typeDesc = getDescriptor(c);
      return getType(typeDesc);
   }

   /**
    * Returns the Java method type corresponding to the given constructor.
    */
   @Nonnull
   public static JavaType getType(@Nonnull Constructor<?> c) {
      String constructorDesc = getConstructorDescriptor(c);
      return getType(constructorDesc);
   }

   /**
    * Returns the Java method type corresponding to the given method.
    */
   @Nonnull
   public static JavaType getType(@Nonnull Method m) {
      String methodDesc = getMethodDescriptor(m);
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
         char car = buf[off++];

         if (car == ')') {
            break;
         }
         else if (car == 'L') {
            while (buf[off++] != ';') {}
            size++;
         }
         else if (car != '[') {
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
    * @return the size of the arguments of the method (plus one for the implicit this argument), argSize, and the size
    * of its return value, retSize, packed into a single int i = <tt>(argSize &lt;&lt; 2) | retSize</tt> (argSize is
    * therefore equal to <tt>i &gt;&gt; 2</tt>, and retSize to <tt>i &amp; 0x03</tt>).
    */
   public static int getArgumentsAndReturnSizes(@Nonnull String desc) {
      int n = 1;
      int c = 1;

      while (true) {
         char car = desc.charAt(c++);

         if (car == ')') {
            car = desc.charAt(c);
            return n << 2 | (car == 'V' ? 0 : car == 'D' || car == 'J' ? 2 : 1);
         }
         else if (car == 'L') {
            while (desc.charAt(c++) != ';') {}
            n++;
         }
         else if (car == '[') {
            while ((car = desc.charAt(c)) == '[') {
               c++;
            }

            if (car == 'D' || car == 'J') {
               n--;
            }
         }
         else if (car == 'D' || car == 'J') {
            n += 2;
         }
         else {
            n++;
         }
      }
   }

   /**
    * Returns the Java type corresponding to the given type descriptor. For method descriptors, buf is supposed to
    * contain nothing more than the descriptor itself.
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
         case '[':
            return createArrayType(buf, off);
         case 'L':
            return createObjectType(buf, off);
         // case '(':
         default:
            return new MethodType(buf, off, buf.length - off);
      }
   }

   private static ArrayType createArrayType(@Nonnull char[] buf, @Nonnegative int off) {
      int len = 1;

      while (buf[off + len] == '[') {
         len++;
      }

      if (buf[off + len] == 'L') {
         len++;

         while (buf[off + len] != ';') {
            len++;
         }
      }

      return new ArrayType(buf, off, len + 1);
   }

   private static ObjectType createObjectType(@Nonnull char[] buf, @Nonnegative int off) {
      int len = 1;

      while (buf[off + len] != ';') {
         len++;
      }

      return new ObjectType(buf, off + 1, len - 1);
   }

   // ------------------------------------------------------------------------
   // Accessors
   // ------------------------------------------------------------------------

   /**
    * Returns the {@link Sort} of this Java type.
    */
   public int getSort() { return sort; }

   /**
    * Returns the number of dimensions of this array type. This method should only be used for an array type.
    */
   public int getDimensions() { throw new UnsupportedOperationException("Not an ArrayType"); }

   /**
    * Returns the type of the elements of this array type. This method should only be used for an array type.
    */
   @Nonnull
   public JavaType getElementType() { throw new UnsupportedOperationException("Not an ArrayType"); }

   /**
    * Returns the binary name of the class corresponding to this type. This method must not be used on method types.
    */
   public abstract String getClassName();

   /**
    * Returns the internal name of the class corresponding to this object or array type. The internal name of a class is
    * its fully qualified name (as returned by Class.getName(), where '.' are replaced by '/'.
    * This method should only be used for an object or array type.
    *
    * @return the internal name of the class corresponding to this object type.
    */
   @Nonnull
   public String getInternalName() { throw new UnsupportedOperationException("Not a ReferenceType"); }

   // ------------------------------------------------------------------------
   // Conversion to type descriptors
   // ------------------------------------------------------------------------

   /**
    * Returns the descriptor corresponding to this Java type.
    */
   @Nonnull
   public final String getDescriptor() {
      StringBuffer buf = new StringBuffer();
      getDescriptor(buf);
      return buf.toString();
   }

   /**
    * Appends the descriptor corresponding to this Java type to the given string buffer.
    *
    * @param buf the string buffer to which the descriptor must be appended.
    */
   abstract void getDescriptor(@Nonnull StringBuffer buf);

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
      StringBuffer buf = new StringBuffer();
      getDescriptor(buf, aClass);
      return buf.toString();
   }

   /**
    * Returns the descriptor corresponding to the given constructor.
    *
    * @param constructor a {@link Constructor} object.
    */
   @Nonnull
   public static String getConstructorDescriptor(@Nonnull Constructor<?> constructor) {
      Class<?>[] parameters = constructor.getParameterTypes();
      StringBuffer buf = new StringBuffer();
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
      StringBuffer buf = new StringBuffer();
      buf.append('(');

      for (Class<?> parameter : parameters) {
         getDescriptor(buf, parameter);
      }

      buf.append(')');
      getDescriptor(buf, method.getReturnType());
      return buf.toString();
   }

   /**
    * Appends the descriptor of the given class to the given string buffer.
    *
    * @param buf the string buffer to which the descriptor must be appended.
    * @param aClass   the class whose descriptor must be computed.
    */
   private static void getDescriptor(@Nonnull StringBuffer buf, @Nonnull Class<?> aClass) {
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

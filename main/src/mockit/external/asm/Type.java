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

/**
 * A Java field or method type. This class can be used to make it easier to manipulate type and method descriptors.
 *
 * @author Eric Bruneton
 * @author Chris Nokleberg
 */
public final class Type
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

   private static final Type VOID_TYPE    = new Type(Sort.VOID,    'V', 5, 0, 0);
   private static final Type BOOLEAN_TYPE = new Type(Sort.BOOLEAN, 'Z', 0, 5, 1);
   private static final Type CHAR_TYPE    = new Type(Sort.CHAR,    'C', 0, 6, 1);
   private static final Type BYTE_TYPE    = new Type(Sort.BYTE,    'B', 0, 5, 1);
   private static final Type SHORT_TYPE   = new Type(Sort.SHORT,   'S', 0, 7, 1);
   private static final Type INT_TYPE     = new Type(Sort.INT,     'I', 0, 0, 1);
   private static final Type FLOAT_TYPE   = new Type(Sort.FLOAT,   'F', 2, 2, 1);
   private static final Type LONG_TYPE    = new Type(Sort.LONG,    'J', 1, 1, 2);
   private static final Type DOUBLE_TYPE  = new Type(Sort.DOUBLE,  'D', 3, 3, 2);

   // ------------------------------------------------------------------------
   // Fields
   // ------------------------------------------------------------------------

   /**
    * The sort of this Java type.
    */
   private final int sort;

   /**
    * A buffer containing the internal name of this Java type. This field is only used for reference types.
    */
   private final char[] buf;

   /**
    * The offset of the internal name of this Java type in {@link #buf buf} or, for primitive types, the size,
    * descriptor and getOpcode offsets for this type (byte 0 contains the size, byte 1 the offset for IALOAD or IASTORE,
    * byte 2 the offset for all other instructions, byte 3 the descriptor).
    */
   private final int off;

   /**
    * The length of the internal name of this Java type.
    */
   private final int len;

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
         default:      return ArrayElementType.DOUBLE;
      }
   }

   // ------------------------------------------------------------------------
   // Constructors
   // ------------------------------------------------------------------------

   /**
    * Constructs a primitive type.
    *
    * @param sort the sort of the primitive type to be constructed.
    * @param desc the primitive type descriptor
    * @param loadOrStoreOffset opcode offset for a IALOAD or IASTORE instruction.
    * @param otherOffset opcode offset for any other instruction.
    * @param size the size in words of the primitive type
    */
   private Type(int sort, char desc, int loadOrStoreOffset, int otherOffset, int size) {
      this.sort = sort;
      buf = null;
      off = (desc << 24) | (loadOrStoreOffset << 16) | (otherOffset << 8) | size;
      len = 1;
   }

   /**
    * Constructs a reference type.
    *
    * @param sort the sort of the reference type to be constructed.
    * @param buf  a buffer containing the descriptor of the previous type.
    * @param off  the offset of this descriptor in the previous buffer.
    * @param len  the length of this descriptor.
    */
   private Type(int sort, char[] buf, int off, int len) {
      this.sort = sort;
      this.buf = buf;
      this.off = off;
      this.len = len;
   }

   /**
    * Returns the Java type corresponding to the given type descriptor.
    *
    * @param typeDescriptor a field or method type descriptor.
    */
   public static Type getType(String typeDescriptor) {
      return getType(typeDescriptor.toCharArray(), 0);
   }

   /**
    * Returns the Java type corresponding to the given internal name.
    */
   public static Type getObjectType(String internalName) {
      char[] buf = internalName.toCharArray();
      return new Type(buf[0] == '[' ? Sort.ARRAY : Sort.OBJECT, buf, 0, buf.length);
   }

   /**
    * Returns the Java type corresponding to the given method descriptor.
    * Equivalent to <code>Type.getType(methodDescriptor)</code>.
    */
   public static Type getMethodType(String methodDescriptor) {
      return getType(methodDescriptor.toCharArray(), 0);
   }

   /**
    * Returns the Java type corresponding to the given class.
    */
   public static Type getType(Class<?> c) {
      if (c.isPrimitive()) {
         if (c == Integer.TYPE) {
            return INT_TYPE;
         }
         else if (c == Void.TYPE) {
            return VOID_TYPE;
         }
         else if (c == Boolean.TYPE) {
            return BOOLEAN_TYPE;
         }
         else if (c == Byte.TYPE) {
            return BYTE_TYPE;
         }
         else if (c == Character.TYPE) {
            return CHAR_TYPE;
         }
         else if (c == Short.TYPE) {
            return SHORT_TYPE;
         }
         else if (c == Double.TYPE) {
            return DOUBLE_TYPE;
         }
         else if (c == Float.TYPE) {
            return FLOAT_TYPE;
         }
         else /* if (c == Long.TYPE) */ {
            return LONG_TYPE;
         }
      }

      return getType(getDescriptor(c));
   }

   /**
    * Returns the Java method type corresponding to the given constructor.
    */
   public static Type getType(Constructor<?> c) {
      return getType(getConstructorDescriptor(c));
   }

   /**
    * Returns the Java method type corresponding to the given method.
    */
   public static Type getType(Method m) {
      return getType(getMethodDescriptor(m));
   }

   /**
    * Returns the Java types corresponding to the argument types of the given method descriptor.
    */
   public static Type[] getArgumentTypes(String methodDescriptor) {
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
            ++size;
         }
         else if (car != '[') {
            ++size;
         }
      }

      Type[] args = new Type[size];
      off = 1;
      size = 0;

      while (buf[off] != ')') {
         args[size] = getType(buf, off);
         off += args[size].len + (args[size].sort == Sort.OBJECT ? 2 : 0);
         size += 1;
      }

      return args;
   }

   /**
    * Returns the Java type corresponding to the return type of the given method descriptor.
    */
   public static Type getReturnType(String methodDescriptor) {
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
   public static int getArgumentsAndReturnSizes(String desc) {
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
            n += 1;
         }
         else if (car == '[') {
            while ((car = desc.charAt(c)) == '[') {
               ++c;
            }

            if (car == 'D' || car == 'J') {
               n -= 1;
            }
         }
         else if (car == 'D' || car == 'J') {
            n += 2;
         }
         else {
            n += 1;
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
   private static Type getType(char[] buf, int off)
   {
      int len;

      switch (buf[off]) {
         case 'V':
            return VOID_TYPE;
         case 'Z':
            return BOOLEAN_TYPE;
         case 'C':
            return CHAR_TYPE;
         case 'B':
            return BYTE_TYPE;
         case 'S':
            return SHORT_TYPE;
         case 'I':
            return INT_TYPE;
         case 'F':
            return FLOAT_TYPE;
         case 'J':
            return LONG_TYPE;
         case 'D':
            return DOUBLE_TYPE;
         case '[':
            len = 1;

            while (buf[off + len] == '[') {
               ++len;
            }

            if (buf[off + len] == 'L') {
               ++len;

               while (buf[off + len] != ';') {
                  ++len;
               }
            }

            return new Type(Sort.ARRAY, buf, off, len + 1);
         case 'L':
            len = 1;

            while (buf[off + len] != ';') {
               ++len;
            }

            return new Type(Sort.OBJECT, buf, off + 1, len - 1);
         // case '(':
         default:
            return new Type(Sort.METHOD, buf, off, buf.length - off);
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
    * Returns the number of dimensions of this array type. This method should only be used for an array type.
    */
   public int getDimensions() {
      int i = 1;

      //noinspection ConstantConditions
      while (buf[off + i] == '[') {
         ++i;
      }

      return i;
   }

   /**
    * Returns the type of the elements of this array type. This method should only be used for an array type.
    */
   public Type getElementType() {
      return getType(buf, off + getDimensions());
   }

   /**
    * Returns the binary name of the class corresponding to this type. This method must not be used on method types.
    */
   public String getClassName() {
      switch (sort) {
         case Sort.VOID:
            return "void";
         case Sort.BOOLEAN:
            return "boolean";
         case Sort.CHAR:
            return "char";
         case Sort.BYTE:
            return "byte";
         case Sort.SHORT:
            return "short";
         case Sort.INT:
            return "int";
         case Sort.FLOAT:
            return "float";
         case Sort.LONG:
            return "long";
         case Sort.DOUBLE:
            return "double";
         case Sort.ARRAY:
            String className = getElementType().getClassName();
            //noinspection ConstantConditions
            StringBuilder sb = new StringBuilder(className);

            for (int i = getDimensions(); i > 0; --i) {
               sb.append("[]");
            }

            return sb.toString();
         case Sort.OBJECT:
            //noinspection ConstantConditions
            return new String(buf, off, len).replace('/', '.');
         default:
            return null;
      }
   }

   /**
    * Returns the internal name of the class corresponding to this object or array type. The internal name of a class is
    * its fully qualified name (as returned by Class.getName(), where '.' are replaced by '/'.
    * This method should only be used for an object or array type.
    *
    * @return the internal name of the class corresponding to this object type.
    */
   public String getInternalName() {
      //noinspection ConstantConditions
      return new String(buf, off, len);
   }

   // ------------------------------------------------------------------------
   // Conversion to type descriptors
   // ------------------------------------------------------------------------

   /**
    * Returns the descriptor corresponding to this Java type.
    */
   public String getDescriptor() {
      StringBuffer buf = new StringBuffer();
      getDescriptor(buf);
      return buf.toString();
   }

   /**
    * Appends the descriptor corresponding to this Java type to the given string buffer.
    *
    * @param buf the string buffer to which the descriptor must be appended.
    */
   private void getDescriptor(StringBuffer buf) {
      if (this.buf == null) {
         // Descriptor is in byte 3 of 'off' for primitive types (buf == null).
         buf.append((char) ((off & 0xFF000000) >>> 24));
      }
      else if (sort == Sort.OBJECT) {
         buf.append('L');
         buf.append(this.buf, off, len);
         buf.append(';');
      }
      else { // sort == ARRAY || sort == METHOD
         buf.append(this.buf, off, len);
      }
   }

   // -------------------------------------------------------------------------------------------------------
   // Direct conversion from classes to type descriptors, and vice-versa, without intermediate Type objects
   // -------------------------------------------------------------------------------------------------------

   /**
    * Returns the internal name of the given class. The internal name of a class is its fully qualified name, as
    * returned by Class.getName(), where '.' are replaced by '/'.
    *
    * @param c an object or array class.
    */
   public static String getInternalName(Class<?> c) {
      return c.getName().replace('.', '/');
   }

   /**
    * Returns the descriptor corresponding to the given Java type.
    *
    * @param c an object class, a primitive class or an array class.
    */
   public static String getDescriptor(Class<?> c) {
      StringBuffer buf = new StringBuffer();
      getDescriptor(buf, c);
      return buf.toString();
   }

   /**
    * Returns the descriptor corresponding to the given constructor.
    *
    * @param c a {@link Constructor} object.
    */
   public static String getConstructorDescriptor(Constructor<?> c) {
      Class<?>[] parameters = c.getParameterTypes();
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
   public static String getMethodDescriptor(Method m) {
      Class<?>[] parameters = m.getParameterTypes();
      StringBuffer buf = new StringBuffer();
      buf.append('(');

      for (Class<?> parameter : parameters) {
         getDescriptor(buf, parameter);
      }

      buf.append(')');
      getDescriptor(buf, m.getReturnType());
      return buf.toString();
   }

   /**
    * Appends the descriptor of the given class to the given string buffer.
    *
    * @param buf the string buffer to which the descriptor must be appended.
    * @param c   the class whose descriptor must be computed.
    */
   private static void getDescriptor(StringBuffer buf, Class<?> c) {
      Class<?> d = c;

      while (true) {
         if (d.isPrimitive()) {
            char car;

            if (d == Integer.TYPE) {
               car = 'I';
            }
            else if (d == Void.TYPE) {
               car = 'V';
            }
            else if (d == Boolean.TYPE) {
               car = 'Z';
            }
            else if (d == Byte.TYPE) {
               car = 'B';
            }
            else if (d == Character.TYPE) {
               car = 'C';
            }
            else if (d == Short.TYPE) {
               car = 'S';
            }
            else if (d == Double.TYPE) {
               car = 'D';
            }
            else if (d == Float.TYPE) {
               car = 'F';
            }
            else /* if (d == Long.TYPE) */ {
               car = 'J';
            }

            buf.append(car);
            return;
         }
         else if (d.isArray()) {
            buf.append('[');
            d = d.getComponentType();
         }
         else {
            buf.append('L');
            String name = d.getName();
            int len = name.length();

            for (int i = 0; i < len; ++i) {
               char car = name.charAt(i);
               buf.append(car == '.' ? '/' : car);
            }

            buf.append(';');
            return;
         }
      }
   }

   public static Class<?> getPrimitiveType(int typeCode) {
      switch (typeCode) {
         case 'I': return int.class;
         case 'Z': return boolean.class;
         case 'D': return double.class;
         case 'J': return long.class;
         case 'C': return char.class;
         case 'B': return byte.class;
         case 'F': return float.class;
         case 'S': return short.class;
         default: return void.class;
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
   public int getSize() {
      // The size is in byte 0 of 'off' for primitive types (buf == null).
      return buf == null ? off & 0xFF : 1;
   }

   /**
    * Returns a JVM instruction opcode adapted to this Java type. This method must not be used for method types.
    *
    * @param opcode a JVM instruction opcode. This opcode must be one of ILOAD, ISTORE, IALOAD, IASTORE, IADD, ISUB,
    *               IMUL, IDIV, IREM, INEG, ISHL, ISHR, IUSHR, IAND, IOR, IXOR and IRETURN.
    * @return an opcode that is similar to the given opcode, but adapted to this Java type. For example, if this type is
    * <tt>float</tt> and <tt>opcode</tt> is IRETURN, this method returns FRETURN.
    */
   public int getOpcode(int opcode) {
      int offset;

      if (buf != null) {
         offset = 4; // for non-primitive types
      }
      else if (opcode == Opcodes.IALOAD || opcode == Opcodes.IASTORE) { // for IALOAD or IASTORE it's in byte 1 of 'off'
         offset = (off & 0xFF00) >> 8;
      }
      else { // for other instructions it's in byte 2 of 'off'
         offset = (off & 0xFF0000) >> 16;
      }

      return opcode + offset;
   }

   // ------------------------------------------------------------------------
   // Equals, hashCode and toString
   // ------------------------------------------------------------------------

   /**
    * Tests if the given object is equal to this type.
    *
    * @param o the object to be compared to this type.
    * @return <tt>true</tt> if the given object is equal to this type.
    */
   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }

      if (!(o instanceof Type)) {
         return false;
      }

      Type t = (Type) o;

      if (sort != t.sort) {
         return false;
      }

      if (sort >= Sort.ARRAY) {
         if (len != t.len) {
            return false;
         }

         for (int i = off, j = t.off, end = i + len; i < end; i++, j++) {
            //noinspection ConstantConditions
            if (buf[i] != t.buf[j]) {
               return false;
            }
         }
      }

      return true;
   }

   /**
    * Returns a hash code value for this type.
    */
   @Override
   public int hashCode() {
      int hc = 13 * sort;

      if (sort >= Sort.ARRAY) {
         for (int i = off, end = i + len; i < end; i++) {
            //noinspection ConstantConditions
            hc = 17 * (hc + buf[i]);
         }
      }

      return hc;
   }

   /**
    * Returns a string representation of this type.
    *
    * @return the descriptor of this type.
    */
   @Override
   public String toString() {
      return getDescriptor();
   }
}

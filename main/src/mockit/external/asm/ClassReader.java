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

import java.io.*;

/**
 * A Java class parser to make a {@link ClassVisitor} visit an existing class.
 * This class parses a byte array conforming to the Java class file format and calls the appropriate visit methods of a
 * given class visitor for each field, method and bytecode instruction encountered.
 *
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
public final class ClassReader extends AnnotatedReader
{
   /**
    * Flag to skip method code. If this class is set <code>CODE</code> attribute won't be visited.
    * This can be used, for example, to retrieve annotations for methods and method parameters.
    */
   public static final int SKIP_CODE = 1;

   /**
    * Flag to skip the debug information in the class. If this flag is set the debug information of the class is not
    * visited, i.e. the {@link MethodVisitor#visitLocalVariable} and {@link MethodVisitor#visitLineNumber} methods will
    * not be called.
    */
   public static final int SKIP_DEBUG = 2;

   private final FieldReader fieldReader;
   private final MethodReader methodReader;

   // Helper fields.
   ClassVisitor cv;
   private Context context;
   int access;
   private String name;
   private String superClass;
   private String[] interfaces;
   private String signature;
   private String sourceFile;
   private String sourceDebug;
   private String enclosingOwner;
   private String enclosingName;
   private String enclosingDesc;

   /**
    * Constructs a new {@link ClassReader} object.
    *
    * @param bytecode the bytecode of the class to be read.
    */
   public ClassReader(byte[] bytecode) {
      super(bytecode);
      fieldReader = new FieldReader(this);
      methodReader = new MethodReader(this);
   }

   /**
    * Returns the classfile version of the class being read.
    */
   public int getVersion() {
      return readShort(6);
   }

   /**
    * Returns the class's access flags (see {@link Opcodes}). This value may not reflect Deprecated and Synthetic flags
    * when bytecode is before 1.5 and those flags are represented by attributes.
    *
    * @return the class access flags
    * @see ClassVisitor#visit(int, int, String, String, String, String[])
    */
   public int getAccess() {
      return readUnsignedShort(header);
   }

   /**
    * Returns the internal name of the class (see {@link Type#getInternalName()}).
    *
    * @return the internal class name
    * @see ClassVisitor#visit(int, int, String, String, String, String[])
    */
   public String getClassName() {
      return readClass(header + 2);
   }

   /**
    * Returns the internal of name of the super class (see {@link Type#getInternalName()}).
    * For interfaces, the super class is {@link Object}.
    *
    * @return the internal name of super class, or <tt>null</tt> for {@link Object} class.
    * @see ClassVisitor#visit(int, int, String, String, String, String[])
    */
   public String getSuperName() {
      return readClass(header + 4);
   }

   /**
    * Returns the internal names of the class's interfaces (see {@link Type#getInternalName()}).
    *
    * @return the array of internal names for all implemented interfaces or <tt>null</tt>.
    * @see ClassVisitor#visit(int, int, String, String, String, String[])
    */
   public String[] getInterfaces() {
      int index = header + 6;
      int n = readUnsignedShort(index);
      String[] interfaces = new String[n];

      if (n > 0) {
         char[] buf = new char[maxStringLength];

         for (int i = 0; i < n; ++i) {
            index += 2;
            interfaces[i] = readClass(index, buf);
         }
      }

      return interfaces;
   }

   /**
    * Copies the constant pool data into the given {@link ClassWriter}.
    */
   void copyPool(ClassWriter cw) {
      char[] buf = new char[maxStringLength];
      int ll = items.length;
      Item[] items2 = new Item[ll];

      for (int i = 1; i < ll; i++) {
         int index = items[i];
         int tag = b[index - 1];
         Item item = new Item(i);
         int nameType;

         switch (tag) {
            case ConstantPoolItemType.FIELD:
            case ConstantPoolItemType.METH:
            case ConstantPoolItemType.IMETH:
               nameType = items[readUnsignedShort(index + 2)];
               item.set(tag, readClass(index, buf), readUTF8(nameType, buf), readUTF8(nameType + 2, buf));
               break;
            case ConstantPoolItemType.INT:
               item.set(readInt(index));
               break;
            case ConstantPoolItemType.FLOAT:
               item.set(Float.intBitsToFloat(readInt(index)));
               break;
            case ConstantPoolItemType.NAME_TYPE:
               item.set(tag, readUTF8(index, buf), readUTF8(index + 2, buf), null);
               break;
            case ConstantPoolItemType.LONG:
               item.set(readLong(index));
               ++i;
               break;
            case ConstantPoolItemType.DOUBLE:
               item.set(Double.longBitsToDouble(readLong(index)));
               ++i;
               break;
            case ConstantPoolItemType.UTF8: {
               String s = strings[i];

               if (s == null) {
                  index = items[i];
                  s = strings[i] = readUTF(index + 2, readUnsignedShort(index), buf);
               }

               item.set(tag, s, null, null);
               break;
            }
            case ConstantPoolItemType.HANDLE: {
               int fieldOrMethodRef = items[readUnsignedShort(index + 1)];
               nameType = items[readUnsignedShort(fieldOrMethodRef + 2)];
               item.set(
                  ConstantPoolItemType.HANDLE_BASE + readByte(index), readClass(fieldOrMethodRef, buf),
                  readUTF8(nameType, buf), readUTF8(nameType + 2, buf));
               break;
            }
            case ConstantPoolItemType.INDY:
               if (!cw.bootstrapMethods.hasMethods()) {
                  cw.bootstrapMethods.copyBootstrapMethods(items2, buf);
               }

               nameType = items[readUnsignedShort(index + 2)];
               item.set(readUTF8(nameType, buf), readUTF8(nameType + 2, buf), readUnsignedShort(index));
               break;
            // case ConstantPoolItemType.STR|CLASS|MTYPE:
            default:
               item.set(tag, readUTF8(index, buf), null, null);
               break;
         }

         int index2 = item.hashCode % items2.length;
         item.next = items2[index2];
         items2[index2] = item;
      }

      int off = items[1] - 1;
      cw.constantPool.copy(b, off, header, items2);
   }

   /**
    * Constructs a new {@link ClassReader} object.
    *
    * @param is an input stream from which to read the class.
    * @throws IOException if a problem occurs during reading.
    */
   public ClassReader(InputStream is) throws IOException {
      this(readClass(is));
   }

   /**
    * Constructs a new {@link ClassReader} object.
    *
    * @param name the binary qualified name of the class to be read.
    * @throws IOException if an exception occurs during reading.
    */
   public ClassReader(String name) throws IOException {
      this(readClass(ClassLoader.getSystemResourceAsStream(name.replace('.', '/') + ".class")));
   }

   /**
    * Makes the given visitor visit the Java class of this {@link ClassReader}, all attributes included.
    */
   public void accept(ClassVisitor cv) {
      accept(cv, 0);
   }

   /**
    * Makes the given visitor visit the Java class of this {@link ClassReader}.
    *
    * @param cv    the visitor that must visit this class.
    * @param flags option flags that can be used to modify the default behavior of this class.
    *              See {@link #SKIP_CODE}, {@link #SKIP_DEBUG}.
    */
   public void accept(ClassVisitor cv, int flags) {
      this.cv = cv;

      char[] c = new char[maxStringLength]; // buffer used to read strings
      context = new Context(flags, c);

      readClassDeclaration();
      readInterfaces();

      // Reads the class attributes.
      signature = null;
      sourceFile = null;
      sourceDebug = null;
      enclosingOwner = null;
      enclosingName = null;
      enclosingDesc = null;
      int annotations = 0;
      int innerClasses = 0;

      int u = getAttributesStartIndex();

      for (int i = readUnsignedShort(u); i > 0; --i) {
         String attrName = readUTF8(u + 2, c);

         if ("SourceFile".equals(attrName)) {
            sourceFile = readUTF8(u + 8, c);
         }
         else if ("InnerClasses".equals(attrName)) {
            innerClasses = u + 8;
         }
         else if ("EnclosingMethod".equals(attrName)) {
            readEnclosingMethodInfo(u);
         }
         else if ("Signature".equals(attrName)) {
            signature = readUTF8(u + 8, c);
         }
         else if ("RuntimeVisibleAnnotations".equals(attrName)) {
            annotations = u + 8;
         }
         else if ("Deprecated".equals(attrName)) {
            access = Access.asDeprecated(access);
         }
         else if ("Synthetic".equals(attrName)) {
            access = Access.asSynthetic(access);
         }
         else if ("SourceDebugExtension".equals(attrName)) {
            int len = readInt(u + 4);
            sourceDebug = readUTF(u + 8, len, new char[len]);
         }
         else if ("BootstrapMethods".equals(attrName)) {
            readBootstrapMethods(context, u);
         }

         u += 6 + readInt(u + 4);
      }

      readClass();
      readSourceAndDebugInfo();
      readOuterClass();
      readAnnotations(annotations);
      readInnerClasses(innerClasses);
      readFieldsAndMethods();
      cv.visitEnd();
   }

   private void readClassDeclaration() {
      int u = header;
      char[] c = context.buffer;
      access = readUnsignedShort(u);
      name = readClass(u + 2, c);
      superClass = readClass(u + 4, c);
   }

   private void readInterfaces() {
      int u = header;
      char[] c = context.buffer;
      interfaces = new String[readUnsignedShort(u + 6)];
      u += 8;

      for (int i = 0; i < interfaces.length; ++i) {
         interfaces[i] = readClass(u, c);
         u += 2;
      }
   }

   private void readEnclosingMethodInfo(int u) {
      char[] c = context.buffer;
      enclosingOwner = readClass(u + 8, c);
      int item = readUnsignedShort(u + 10);

      if (item != 0) {
         enclosingName = readUTF8(items[item], c);
         enclosingDesc = readUTF8(items[item] + 2, c);
      }
   }

   private void readClass() {
      int version = readInt(items[1] - 7);
      cv.visit(version, access, name, signature, superClass, interfaces);
   }

   private void readSourceAndDebugInfo() {
      if (context.readDebugInfo() && (sourceFile != null || sourceDebug != null)) {
         cv.visitSource(sourceFile, sourceDebug);
      }
   }

   private void readOuterClass() {
      if (enclosingOwner != null) {
         cv.visitOuterClass(enclosingOwner, enclosingName, enclosingDesc);
      }
   }

   private void readAnnotations(int annotations) {
      if (annotations != 0) {
         char[] c = context.buffer;

         for (int i = readUnsignedShort(annotations), v = annotations + 2; i > 0; --i) {
            String desc = readUTF8(v, c);
            AnnotationVisitor av = cv.visitAnnotation(desc);
            v = annotationReader.readAnnotationValues(v + 2, c, true, av);
         }
      }
   }

   private void readInnerClasses(int innerClasses) {
      if (innerClasses != 0) {
         int v = innerClasses + 2;
         char[] c = context.buffer;

         for (int i = readUnsignedShort(innerClasses); i > 0; --i) {
            String name = readClass(v, c);
            String outerName = readClass(v + 2, c);
            String innerName = readUTF8(v + 4, c);
            int access = readUnsignedShort(v + 6);
            cv.visitInnerClass(name, outerName, innerName, access);
            v += 8;
         }
      }
   }

   private void readFieldsAndMethods() {
      int u = header + 10 + 2 * interfaces.length;

      for (int i = readUnsignedShort(u - 2); i > 0; --i) {
         u = fieldReader.readField(cv, context, u);
      }

      u += 2;

      for (int i = readUnsignedShort(u - 2); i > 0; --i) {
         u = methodReader.readMethod(context, u);
      }
   }

   private void readBootstrapMethods(Context context, int u) {
      int[] bootstrapMethods = new int[readUnsignedShort(u + 8)];

      for (int j = 0, v = u + 10; j < bootstrapMethods.length; j++) {
         bootstrapMethods[j] = v;
         v += 2 + readUnsignedShort(v + 2) << 1;
      }

      context.bootstrapMethods = bootstrapMethods;
   }

   /**
    * Returns the start index of the attribute_info structure of this class.
    */
   int getAttributesStartIndex() {
      // Skips the header.
      int u = header + 8 + readUnsignedShort(header + 6) * 2;

      // Skips fields and methods.
      for (int i = readUnsignedShort(u); i > 0; --i) {
         for (int j = readUnsignedShort(u + 8); j > 0; --j) {
            u += 6 + readInt(u + 12);
         }

         u += 8;
      }

      u += 2;

      for (int i = readUnsignedShort(u); i > 0; --i) {
         for (int j = readUnsignedShort(u + 8); j > 0; --j) {
            u += 6 + readInt(u + 12);
         }

         u += 8;
      }

      // The attribute_info structure starts just after the methods.
      return u + 2;
   }
}

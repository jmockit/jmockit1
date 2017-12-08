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
import javax.annotation.*;

import static mockit.external.asm.ConstantPoolItemType.*;

/**
 * A Java class parser to make a {@link ClassVisitor} visit an existing class.
 * This class parses a byte array conforming to the Java class file format and calls the appropriate visit methods of a
 * given class visitor for each field, method and bytecode instruction encountered.
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

   private static final String[] NO_INTERFACES = {};

   // Helper fields.
   ClassVisitor cv;
   boolean readCode;
   boolean readDebugInfo;

   /**
    * The start index of each bootstrap method.
    */
   @Nullable int[] bootstrapMethods;

   private int access;
   private String name;
   @Nullable private String superClass;
   @Nonnull private String[] interfaces = NO_INTERFACES;
   @Nullable private String signature;
   @Nullable private String sourceFile;
   @Nullable private EnclosingMethod enclosingMethod;
   @Nonnegative private int annotationsCodeIndex;
   @Nonnegative private int innerClassesCodeIndex;

   /**
    * Initializes a new class reader with the given bytecode array for a classfile.
    */
   public ClassReader(@Nonnull byte[] code) { super(code); }

   /**
    * Initializes a new class reader whose classfile bytecode array is read from the given input stream.
    */
   public ClassReader(@Nonnull InputStream is) throws IOException {
      this(readClass(is));
   }

   @Nonnull
   private static byte[] readClass(@Nonnull InputStream is) throws IOException {
      try {
         byte[] b = new byte[is.available()];
         int len = 0;

         while (true) {
            int n = is.read(b, len, b.length - len);

            if (n == -1) {
               if (len < b.length) {
                  byte[] c = new byte[len];
                  System.arraycopy(b, 0, c, 0, len);
                  b = c;
               }

               return b;
            }

            len += n;

            if (len == b.length) {
               int last = is.read();

               if (last < 0) {
                  return b;
               }

               byte[] c = new byte[b.length + 1000];
               System.arraycopy(b, 0, c, 0, len);
               c[len++] = (byte) last;
               b = c;
            }
         }
      }
      finally {
         is.close();
      }
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
    * Returns the internal name of the class.
    *
    * @return the internal class name
    * @see ClassVisitor#visit(int, int, String, String, String, String[])
    */
   public String getClassName() {
      return readClass(header + 2);
   }

   /**
    * Returns the internal of name of the super class.
    * For interfaces, the super class is {@link Object}.
    *
    * @return the internal name of super class, or <tt>null</tt> for {@link Object} class.
    * @see ClassVisitor#visit(int, int, String, String, String, String[])
    */
   @Nullable
   public String getSuperName() {
      return readClass(header + 4);
   }

   /**
    * Returns the internal names of the class's interfaces.
    *
    * @return the array of internal names for all implemented interfaces or <tt>null</tt>.
    * @see ClassVisitor#visit(int, int, String, String, String, String[])
    */
   @Nonnull
   public String[] getInterfaces() {
      int codeIndex = header + 6;
      int interfaceCount = readUnsignedShort(codeIndex);
      String[] interfaces = new String[interfaceCount];

      if (interfaceCount > 0) {
         for (int i = 0; i < interfaceCount; i++) {
            codeIndex += 2;
            interfaces[i] = readClass(codeIndex);
         }
      }

      return interfaces;
   }

   /**
    * Returns the bytecode array of the Java classfile that was read.
    */
   @Nonnull
   public byte[] getBytecode() { return code; }

   /**
    * Copies the constant pool data into the given {@link ClassWriter}.
    */
   @SuppressWarnings({"OverlyComplexMethod", "OverlyLongMethod"})
   void copyPool(@Nonnull ClassWriter cw) {
      int itemCount = items.length;
      Item[] items2 = new Item[itemCount];

      for (int itemIndex = 1; itemIndex < itemCount; itemIndex++) {
         int itemCodeIndex = items[itemIndex];
         int itemType = code[itemCodeIndex - 1];
         Item item;

         switch (itemType) {
            case FIELD: case METH: case IMETH:
               item = copyFieldOrMethodReferenceItem(itemType, itemCodeIndex, itemIndex);
               break;
            case INT:
               item = copyIntItem(itemCodeIndex, itemIndex);
               break;
            case FLOAT:
               item = copyFloatItem(itemCodeIndex, itemIndex);
               break;
            case NAME_TYPE:
               item = copyNameAndTypeItem(itemType, itemCodeIndex, itemIndex);
               break;
            case LONG:
               item = copyLongItem(itemCodeIndex, itemIndex);
               itemIndex++;
               break;
            case DOUBLE:
               item = copyDoubleItem(itemCodeIndex, itemIndex);
               itemIndex++;
               break;
            case UTF8:
               item = copyUTF8Item(itemType, itemIndex);
               break;
            case HANDLE:
               item = copyHandleItem(itemCodeIndex, itemIndex);
               break;
            case INDY:
               item = copyInvokeDynamicItem(cw.bootstrapMethods, items2, itemCodeIndex, itemIndex);
               break;
            // case STR|CLASS|MTYPE:
            default:
               item = copyNameReferenceItem(itemType, itemCodeIndex, itemIndex);
         }

         item.setNext(items2);
      }

      int off = items[1] - 1;
      cw.cp.copy(code, off, header, items2);
   }

   @Nonnull
   private Item copyIntItem(@Nonnegative int codeIndex, @Nonnegative int itemIndex) {
      int itemValue = readInt(codeIndex);
      IntItem item = new IntItem(itemIndex);
      item.set(itemValue);
      return item;
   }

   @Nonnull
   private Item copyLongItem(@Nonnegative int codeIndex, @Nonnegative int itemIndex) {
      long itemValue = readLong(codeIndex);
      LongItem item = new LongItem(itemIndex);
      item.set(itemValue);
      return item;
   }

   @Nonnull
   private Item copyFloatItem(@Nonnegative int codeIndex, @Nonnegative int itemIndex) {
      float itemValue = readFloat(codeIndex);
      FloatItem item = new FloatItem(itemIndex);
      item.set(itemValue);
      return item;
   }

   @Nonnull
   private Item copyDoubleItem(@Nonnegative int codeIndex, @Nonnegative int itemIndex) {
      double itemValue = readDouble(codeIndex);
      DoubleItem item = new DoubleItem(itemIndex);
      item.set(itemValue);
      return item;
   }

   @Nonnull
   private Item copyNameAndTypeItem(int itemType, @Nonnegative int codeIndex, @Nonnegative int itemIndex) {
      String name = readUTF8(codeIndex);
      String type = readUTF8(codeIndex + 2);

      ReferenceItem item = new ReferenceItem(itemIndex);
      //noinspection ConstantConditions
      item.set(itemType, name, type, null);
      return item;
   }

   @Nonnull
   private Item copyFieldOrMethodReferenceItem(int type, @Nonnegative int codeIndex, @Nonnegative int itemIndex) {
      int nameType = items[readUnsignedShort(codeIndex + 2)];
      String classDesc = readClass(codeIndex);
      String methodName = readUTF8(nameType);
      String methodDesc = readUTF8(nameType + 2);

      ReferenceItem item = new ReferenceItem(itemIndex);
      //noinspection ConstantConditions
      item.set(type, classDesc, methodName, methodDesc);
      return item;
   }

   @Nonnull
   private Item copyUTF8Item(int type, @Nonnegative int itemIndex) {
      String string = readString(itemIndex);

      ReferenceItem item = new ReferenceItem(itemIndex);
      item.set(type, string, null, null);
      return item;
   }

   @Nonnull
   private Item copyHandleItem(@Nonnegative int codeIndex, @Nonnegative int itemIndex) {
      int fieldOrMethodRef = items[readUnsignedShort(codeIndex + 1)];
      int nameType = items[readUnsignedShort(fieldOrMethodRef + 2)];

      int type = HANDLE_BASE + readByte(codeIndex);
      String classDesc = readClass(fieldOrMethodRef);
      String name = readUTF8(nameType);
      String desc = readUTF8(nameType + 2);

      ReferenceItem item = new ReferenceItem(itemIndex);
      //noinspection ConstantConditions
      item.set(type, classDesc, name, desc);
      return item;
   }

   @Nonnull
   private Item copyInvokeDynamicItem(
      @Nonnull BootstrapMethods bootstrapMethods, @Nonnull Item[] items2, @Nonnegative int codeIndex,
      @Nonnegative int itemIndex
   ) {
      bootstrapMethods.copyBootstrapMethods(this, items2);

      int nameType = items[readUnsignedShort(codeIndex + 2)];
      String name = readUTF8(nameType);
      String desc = readUTF8(nameType + 2);
      int bsmIndex = readUnsignedShort(codeIndex);

      InvokeDynamicItem item = new InvokeDynamicItem(itemIndex);
      //noinspection ConstantConditions
      item.set(name, desc, bsmIndex);
      return item;
   }

   @Nonnull
   private Item copyNameReferenceItem(int type, @Nonnegative int codeIndex, @Nonnegative int itemIndex) {
      String string = readUTF8(codeIndex);

      ReferenceItem item = new ReferenceItem(itemIndex);
      //noinspection ConstantConditions
      item.set(type, string, null, null);
      return item;
   }

   /**
    * Makes the given visitor visit the Java class of this Class Reader, all attributes included.
    */
   public void accept(ClassVisitor cv) {
      accept(cv, 0);
   }

   /**
    * Makes the given visitor visit the Java class of this Class Reader.
    *
    * @param cv    the visitor that must visit this class.
    * @param flags option flags that can be used to modify the default behavior of this class.
    *              See {@link #SKIP_CODE}, {@link #SKIP_DEBUG}.
    */
   public void accept(@Nonnull ClassVisitor cv, int flags) {
      this.cv = cv;
      setFlags(flags);
      readClassDeclaration();
      readInterfaces();
      readClassAttributes();
      readClass();
      readSourceFileName();
      readOuterClass();
      readAnnotations();
      readInnerClasses();
      readFieldsAndMethods();
      cv.visitEnd();
   }

   private void readClassAttributes() {
      signature = null;
      sourceFile = null;
      enclosingMethod = null;
      annotationsCodeIndex = 0;
      innerClassesCodeIndex = 0;

      int codeIndex = getAttributesStartIndex();

      for (int attributeCount = readUnsignedShort(codeIndex); attributeCount > 0; attributeCount--) {
         String attrName = readUTF8(codeIndex + 2);

         if ("SourceFile".equals(attrName)) {
            sourceFile = readUTF8(codeIndex + 8);
         }
         else if ("InnerClasses".equals(attrName)) {
            innerClassesCodeIndex = codeIndex + 8;
         }
         else if ("EnclosingMethod".equals(attrName)) {
            enclosingMethod = new EnclosingMethod(this, codeIndex);
         }
         else if ("Signature".equals(attrName)) {
            signature = readUTF8(codeIndex + 8);
         }
         else if ("RuntimeVisibleAnnotations".equals(attrName)) {
            annotationsCodeIndex = codeIndex + 8;
         }
         else if ("Deprecated".equals(attrName)) {
            access = Access.asDeprecated(access);
         }
         else if ("Synthetic".equals(attrName)) {
            access = Access.asSynthetic(access);
         }
         else if ("BootstrapMethods".equals(attrName)) {
            readBootstrapMethods(codeIndex);
         }

         codeIndex += 6 + readInt(codeIndex + 4);
      }
   }

   private void setFlags(int flags) {
      readCode = (flags & SKIP_CODE) == 0;
      readDebugInfo = (flags & SKIP_DEBUG) == 0;
   }

   private void readClassDeclaration() {
      int u = header;
      access = readUnsignedShort(u);
      name = readClass(u + 2);
      superClass = readClass(u + 4);
   }

   private void readInterfaces() {
      int u = header;
      int interfaceCount = readUnsignedShort(u + 6);

      if (interfaceCount == 0) {
         return;
      }

      interfaces = new String[interfaceCount];
      u += 8;

      for (int i = 0; i < interfaceCount; i++) {
         interfaces[i] = readClass(u);
         u += 2;
      }
   }

   private void readBootstrapMethods(@Nonnegative int u) {
      int bsmCount = readUnsignedShort(u + 8);
      bootstrapMethods = new int[bsmCount];

      for (int i = 0, v = u + 10; i < bsmCount; i++) {
         bootstrapMethods[i] = v;
         v += 2 + readUnsignedShort(v + 2) << 1;
      }
   }

   private void readClass() {
      int version = readInt(items[1] - 7);
      cv.visit(version, access, name, signature, superClass, interfaces);
   }

   private void readSourceFileName() {
      if (readDebugInfo && (sourceFile != null)) {
         cv.visitSource(sourceFile);
      }
   }

   private void readOuterClass() {
      if (enclosingMethod != null) {
         cv.visitOuterClass(enclosingMethod.owner, enclosingMethod.name, enclosingMethod.desc);
      }
   }

   private void readAnnotations() {
      int startIndex = annotationsCodeIndex;

      if (startIndex != 0) {
         int codeIndex = startIndex + 2;

         for (int annotationCount = readUnsignedShort(startIndex); annotationCount > 0; annotationCount--) {
            String desc = readUTF8(codeIndex);
            @SuppressWarnings("ConstantConditions") AnnotationVisitor av = cv.visitAnnotation(desc);
            codeIndex = annotationReader.readNamedAnnotationValues(codeIndex + 2, av);
         }
      }
   }

   private void readInnerClasses() {
      int startIndex = innerClassesCodeIndex;

      if (startIndex != 0) {
         int codeIndex = startIndex + 2;

         for (int innerClassCount = readUnsignedShort(startIndex); innerClassCount > 0; innerClassCount--) {
            String name = readClass(codeIndex);
            String outerName = readClass(codeIndex + 2);
            String innerName = readUTF8(codeIndex + 4);
            int access = readUnsignedShort(codeIndex + 6);

            //noinspection ConstantConditions
            cv.visitInnerClass(name, outerName, innerName, access);
            codeIndex += 8;
         }
      }
   }

   private void readFieldsAndMethods() {
      int codeIndex = header + 8 + 2 * interfaces.length;

      FieldReader fieldReader = new FieldReader(this);
      codeIndex = fieldReader.readFields(codeIndex);

      MethodReader methodReader = new MethodReader(this);
      methodReader.readMethods(codeIndex);
   }

   /**
    * Returns the start index of the attribute_info structure of this class.
    */
   @Nonnegative
   int getAttributesStartIndex() {
      // Skips the header.
      int codeIndex = header + 8 + readUnsignedShort(header + 6) * 2;

      codeIndex = skipClassMembers(codeIndex); // fields
      codeIndex = skipClassMembers(codeIndex); // methods

      // The attribute_info structure starts just after the methods.
      return codeIndex;
   }

   @Nonnegative
   private int skipClassMembers(@Nonnegative int codeIndex) {
      for (int memberCount = readUnsignedShort(codeIndex); memberCount > 0; memberCount--) {
         codeIndex = skipMemberAttributes(codeIndex) + 8;
      }

      return codeIndex + 2;
   }

   @Nonnegative
   private int skipMemberAttributes(@Nonnegative int codeIndex) {
      for (int attributeCount = readUnsignedShort(codeIndex + 8); attributeCount > 0; attributeCount--) {
         codeIndex += 6 + readInt(codeIndex + 12);
      }

      return codeIndex;
   }
}

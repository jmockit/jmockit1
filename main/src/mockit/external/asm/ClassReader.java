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

/**
 * A Java class parser to make a {@link ClassVisitor} visit an existing class.
 * This class parses a byte array conforming to the Java class file format and calls the appropriate visit methods of a
 * given class visitor for each field, method and bytecode instruction encountered.
 */
public final class ClassReader extends AnnotatedReader
{
   public interface Flags
   {
      /**
       * Flag to skip method code. If this class is set <code>CODE</code> attribute won't be visited.
       * This can be used, for example, to retrieve annotations for methods and method parameters.
       */
      int SKIP_CODE = 1;

      /**
       * Flag to skip the debug information in the class. If this flag is set the debug information of the class is not
       * visited, i.e. the {@link MethodVisitor#visitLocalVariable} and {@link MethodVisitor#visitLineNumber} methods
       * will not be called.
       */
      int SKIP_DEBUG = 2;

      int SKIP_CODE_DEBUG = SKIP_CODE + SKIP_DEBUG;

      int SKIP_INNER_CLASSES = 4;
   }

   private static final String[] NO_INTERFACES = {};

   // Helper fields.
   ClassVisitor cv;
   @Nonnegative int flags;

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
    * Returns the classfile version of the class being read (see {@link ClassVersion}).
    */
   public int getVersion() {
      return readShort(6);
   }

   /**
    * Returns the class's access flags (see {@link Access}).
    */
   public int getAccess() {
      return readUnsignedShort(header);
   }

   /**
    * Returns the internal name of the class.
    */
   @Nonnull
   public String getClassName() {
      return readNonnullClass(header + 2);
   }

   /**
    * Returns the internal of name of the super class. For interfaces, the super class is {@link Object}.
    *
    * @return the internal name of super class, or <tt>null</tt> for {@link Object} class.
    */
   @Nullable
   public String getSuperName() {
      return readClass(header + 4);
   }

   /**
    * Returns the internal names of the class's interfaces.
    */
   @Nonnull
   public String[] getInterfaces() {
      codeIndex = header + 6;
      int interfaceCount = readUnsignedShort();
      String[] interfaces = new String[interfaceCount];

      for (int i = 0; i < interfaceCount; i++) {
         interfaces[i] = readNonnullClass();
      }

      return interfaces;
   }

   /**
    * Returns the bytecode array of the Java classfile that was read.
    */
   @Nonnull
   public byte[] getBytecode() { return code; }

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
    * @param flags option flags that can be used to modify the default behavior of this class. See {@link Flags}.
    */
   public void accept(@Nonnull ClassVisitor cv, @Nonnegative int flags) {
      this.cv = cv;
      this.flags = flags;
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
         codeIndex += 2;

         String attrName = readNonnullUTF8(codeIndex);
         codeIndex += 2;

         int offsetToNextAttribute = readInt(codeIndex) - 2;
         codeIndex += 4;

         if ("SourceFile".equals(attrName)) {
            sourceFile = readNonnullUTF8(codeIndex);
         }
         else if ("InnerClasses".equals(attrName)) {
            if ((flags & Flags.SKIP_INNER_CLASSES) == 0) {
               innerClassesCodeIndex = codeIndex;
            }
         }
         else if ("EnclosingMethod".equals(attrName)) {
            enclosingMethod = new EnclosingMethod(this, codeIndex);
         }
         else if ("Signature".equals(attrName)) {
            signature = readNonnullUTF8(codeIndex);
         }
         else if ("RuntimeVisibleAnnotations".equals(attrName)) {
            annotationsCodeIndex = codeIndex;
         }
         else if ("BootstrapMethods".equals(attrName)) {
            readBootstrapMethods(codeIndex);
         }
         else {
            readAccessAttribute(attrName);
         }

         codeIndex += offsetToNextAttribute;
      }
   }

   private void readAccessAttribute(@Nonnull String attrName) {
      if ("Deprecated".equals(attrName)) {
         access = Access.asDeprecated(access);
      }
      else if ("Synthetic".equals(attrName)) {
         access = Access.asSynthetic(access);
      }
   }

   private void readClassDeclaration() {
      int codeIndex = header;
      access = readUnsignedShort(codeIndex);
      name = readNonnullClass(codeIndex + 2);
      superClass = readClass(codeIndex + 4);
   }

   private void readInterfaces() {
      int codeIndex = header;
      int interfaceCount = readUnsignedShort(codeIndex + 6);

      if (interfaceCount == 0) {
         return;
      }

      interfaces = new String[interfaceCount];
      codeIndex += 8;

      for (int i = 0; i < interfaceCount; i++) {
         interfaces[i] = readNonnullClass(codeIndex);
         codeIndex += 2;
      }
   }

   private void readBootstrapMethods(@Nonnegative int codeIndex) {
      int bsmCount = readUnsignedShort(codeIndex);
      codeIndex += 2;

      bootstrapMethods = new int[bsmCount];

      for (int i = 0; i < bsmCount; i++) {
         bootstrapMethods[i] = codeIndex;
         int codeOffset = 2 + readUnsignedShort(codeIndex + 2);
         codeIndex += codeOffset << 1;
      }
   }

   private void readClass() {
      int version = readInt(items[1] - 7);
      cv.visit(version, access, name, signature, superClass, interfaces);
   }

   private void readSourceFileName() {
      if ((flags & Flags.SKIP_DEBUG) == 0 && sourceFile != null) {
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
            String desc = readNonnullUTF8(codeIndex);
            AnnotationVisitor av = cv.visitAnnotation(desc);
            codeIndex = annotationReader.readNamedAnnotationValues(codeIndex + 2, av);
         }
      }
   }

   private void readInnerClasses() {
      int startIndex = innerClassesCodeIndex;

      if (startIndex != 0) {
         int codeIndex = startIndex + 2;

         for (int innerClassCount = readUnsignedShort(startIndex); innerClassCount > 0; innerClassCount--) {
            String name = readNonnullClass(codeIndex);
            String outerName = readClass(codeIndex + 2);
            String innerName = readUTF8(codeIndex + 4);
            int access = readUnsignedShort(codeIndex + 6);

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

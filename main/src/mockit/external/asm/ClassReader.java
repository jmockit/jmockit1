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

   /**
    * Start index of the class header information (access, name...) in {@link #code}.
    */
   @Nonnegative final int header;

   ClassVisitor cv;
   @Nonnegative int flags;
   @Nonnull private String[] interfaces = NO_INTERFACES;
   @Nullable private String sourceFileName;
   @Nullable private EnclosingMethod enclosingMethod;
   @Nonnegative private int innerClassesCodeIndex;
   @Nonnegative private int attributesCodeIndex;

   /**
    * The start index of each bootstrap method.
    */
   @Nullable int[] bootstrapMethods;

   /**
    * Initializes a new class reader with the given bytecode array for a classfile.
    */
   public ClassReader(@Nonnull byte[] code) {
      super(code);
      header = codeIndex; // the class header information starts just after the constant pool
   }

   /**
    * Returns the classfile version of the class being read (see {@link ClassVersion}).
    */
   public int getVersion() {
      codeIndex = 6;
      return readShort();
   }

   /**
    * Returns the class's access flags (see {@link Access}).
    */
   public int getAccess() {
      codeIndex = header;
      return readUnsignedShort();
   }

   /**
    * Returns the internal of name of the super class. For interfaces, the super class is {@link Object}.
    */
   @Nonnull
   public String getSuperName() {
      codeIndex = header + 4;
      return readNonnullClass();
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

      int version = getVersion();

      codeIndex = header;
      access = readUnsignedShort();
      String classDesc = readNonnullClass();
      String superClassDesc = readClass();

      readInterfaces();
      readClassAttributes();
      cv.visit(version, access, classDesc, signature, superClassDesc, interfaces);
      visitSourceFileName();
      visitOuterClass();
      readAnnotations(cv);
      readInnerClasses();
      readFieldsAndMethods();

      cv.visitEnd();
   }

   private void readInterfaces() {
      int interfaceCount = readUnsignedShort();

      if (interfaceCount > 0) {
         interfaces = new String[interfaceCount];

         for (int i = 0; i < interfaceCount; i++) {
            interfaces[i] = readNonnullClass();
         }
      }
   }

   private void readClassAttributes() {
      sourceFileName = null;
      enclosingMethod = null;
      innerClassesCodeIndex = 0;
      codeIndex = getAttributesStartIndex();

      readAttributes();
   }

   @Nullable @Override
   Boolean readAttribute(@Nonnull String attributeName) {
      if ("SourceFile".equals(attributeName)) {
         sourceFileName = readNonnullUTF8();
         return true;
      }

      if ("EnclosingMethod".equals(attributeName)) {
         enclosingMethod = new EnclosingMethod(this);
         return true;
      }

      if ("BootstrapMethods".equals(attributeName)) {
         readBootstrapMethods();
         return true;
      }

      if ("InnerClasses".equals(attributeName)) {
         if ((flags & Flags.SKIP_INNER_CLASSES) == 0) {
            innerClassesCodeIndex = codeIndex;
         }

         return false;
      }

      return null;
   }

   private void readBootstrapMethods() {
      int bsmCount = readUnsignedShort();
      bootstrapMethods = new int[bsmCount];

      for (int i = 0; i < bsmCount; i++) {
         bootstrapMethods[i] = codeIndex;
         codeIndex += 2;
         int codeOffset = readUnsignedShort();
         codeIndex += codeOffset << 1;
      }
   }

   private void visitSourceFileName() {
      if ((flags & Flags.SKIP_DEBUG) == 0) {
         cv.visitSource(sourceFileName);
      }
   }

   private void visitOuterClass() {
      if (enclosingMethod != null) {
         cv.visitOuterClass(enclosingMethod.owner, enclosingMethod.name, enclosingMethod.desc);
      }
   }

   private void readInnerClasses() {
      int startIndex = innerClassesCodeIndex;

      if (startIndex != 0) {
         codeIndex = startIndex;

         for (int innerClassCount = readUnsignedShort(); innerClassCount > 0; innerClassCount--) {
            String name = readNonnullClass();
            String outerName = readClass();
            String innerName = readUTF8();
            int access = readUnsignedShort();

            cv.visitInnerClass(name, outerName, innerName, access);
         }
      }
   }

   private void readFieldsAndMethods() {
      codeIndex = getCodeIndexAfterInterfaces(interfaces.length);

      FieldReader fieldReader = new FieldReader(this);
      codeIndex = fieldReader.readFields();

      MethodReader methodReader = new MethodReader(this);
      codeIndex = methodReader.readMethods();
   }

   @Nonnegative
   private int getCodeIndexAfterInterfaces(@Nonnegative int interfaceCount) { return header + 8 + 2 * interfaceCount; }

   /**
    * Returns the start index of the attribute_info structure of this class.
    */
   @Nonnegative
   private int getAttributesStartIndex() {
      if (attributesCodeIndex == 0) {
         skipHeader();
         skipClassMembers(); // fields
         skipClassMembers(); // methods
         attributesCodeIndex = codeIndex;
      }

      return attributesCodeIndex;
   }

   private void skipHeader() {
      int interfaceCount = readUnsignedShort(header + 6);
      codeIndex = getCodeIndexAfterInterfaces(interfaceCount);
   }

   private void skipClassMembers() {
      for (int memberCount = readUnsignedShort(); memberCount > 0; memberCount--) {
         codeIndex += 6; // skips access, name and desc
         skipMemberAttributes();
      }
   }

   private void skipMemberAttributes() {
      for (int attributeCount = readUnsignedShort(); attributeCount > 0; attributeCount--) {
         codeIndex += 2; // skips attribute name
         int codeOffsetToNextAttribute = readInt();
         codeIndex += codeOffsetToNextAttribute;
      }
   }

   boolean positionAtBootstrapMethodsAttribute() {
      codeIndex = getAttributesStartIndex();

      for (int attributeCount = readUnsignedShort(); attributeCount > 0; attributeCount--) {
         String attrName = readNonnullUTF8();

         if ("BootstrapMethods".equals(attrName)) {
            return true;
         }

         int codeOffsetToNextAttribute = readInt();
         codeIndex += codeOffsetToNextAttribute;
      }

      return false;
   }
}

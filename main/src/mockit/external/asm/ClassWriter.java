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

import java.util.*;

import javax.annotation.*;

import mockit.internal.util.*;

/**
 * A {@link ClassVisitor} that generates classes in bytecode form. More precisely this visitor generates a byte array
 * conforming to the Java class file format. It can be used alone, to generate a Java class "from scratch", or with one
 * or more {@link ClassReader} and adapter class visitor to generate a modified class from one or more existing Java
 * classes.
 */
public final class ClassWriter extends ClassVisitor
{
   /**
    * The class reader from which this class writer was constructed.
    */
   @Nonnull final ClassReader cr;

   /**
    * Minor and major version numbers of the class to be generated.
    */
   private int version;

   /**
    * The access flags of this class.
    */
   private int access;

   /**
    * The constant pool item that contains the internal name of this class.
    */
   private int name;

   /**
    * The internal name of this class.
    */
   String thisName;

   /**
    * The constant pool item that contains the signature of this class.
    */
   private int signature;

   /**
    * The constant pool item that contains the internal name of the super class of this class.
    */
   private int superName;

   @Nullable private Interfaces interfaces;
   @Nonnull private final SourceInfo sourceInfo;
   @Nullable private OuterClass outerClass;
   @Nullable private InnerClasses innerClasses;
   @Nonnull final BootstrapMethods bootstrapMethods;

   /**
    * The fields of this class.
    */
   @Nonnull private final List<FieldWriter> fields;

   /**
    * The methods of this class.
    */
   @Nonnull private final List<MethodWriter> methods;

   /**
    * <tt>true</tt> if the stack map frames must be recomputed from scratch.
    * <p/>
    * If this flag is set, then the stack map frames are recomputed from the methods bytecode. The arguments of the
    * {@link MethodVisitor#visitMaxStack} method are also ignored and recomputed from the bytecode. In other words,
    * computeFrames implies computeMaxs.
    */
   private final boolean computeFrames;

   /**
    * Constructs a new ClassWriter object and enables optimizations for "mostly add" bytecode transformations.
    * These optimizations are the following:
    * <ul>
    * <li>The constant pool from the original class is copied as is in the new class, which saves time.
    * New constant pool entries will be added at the end if necessary, but unused constant pool entries <i>won't be
    * removed</i>.</li>
    * <li>Methods that are not transformed are copied as is in the new class, directly from the original class bytecode
    * (i.e. without emitting visit events for all the method instructions), which saves a <i>lot</i> of time.
    * Untransformed methods are detected by the fact that the {@link ClassReader} receives {@link MethodVisitor}
    * objects that come from a ClassWriter (and not from any other {@link ClassVisitor} instance).</li>
    * </ul>
    *
    * @param classReader the {@link ClassReader} used to read the original class. It will be used to copy the entire
    *                    constant pool from the original class and also to copy other fragments of original bytecode
    *                    where applicable.
    */
   public ClassWriter(@Nonnull ClassReader classReader) {
      cp = new ConstantPoolGeneration();
      sourceInfo = new SourceInfo(cp);
      bootstrapMethods = new BootstrapMethods(cp);

      version = classReader.getVersion();
      computeFrames = version >= ClassVersion.V1_7;

      cr = classReader;
      classReader.copyPool(this);

      fields = new ArrayList<FieldWriter>();
      methods = new ArrayList<MethodWriter>();
   }

   // ------------------------------------------------------------------------
   // Implementation of the ClassVisitor base class
   // ------------------------------------------------------------------------

   @Override
   public void visit(
      int version, int access, @Nonnull String name, @Nullable String signature, @Nullable String superName,
      @Nullable String[] interfaces
   ) {
      this.version = version;
      this.access = access;
      this.name = cp.newClass(name);
      thisName = name;

      if (signature != null) {
         this.signature = cp.newUTF8(signature);
      }

      this.superName = superName == null ? 0 : cp.newClass(superName);

      if (interfaces != null && interfaces.length > 0) {
         this.interfaces = new Interfaces(cp, interfaces);
      }

      if (superName != null) {
         ClassLoad.addSuperClass(name, superName);
      }
   }

   @Override
   public void visitSource(@Nullable String file) {
      sourceInfo.add(file);
   }

   @Override
   public void visitOuterClass(@Nonnull String owner, @Nullable String name, @Nullable String desc) {
      outerClass = new OuterClass(cp, owner, name, desc);
   }

   @Nonnull @Override
   public AnnotationVisitor visitAnnotation(@Nonnull String desc) {
      return addAnnotation(desc);
   }

   @Override
   public void visitInnerClass(
      @Nonnull String name, @Nullable String outerName, @Nullable String innerName, int access
   ) {
      if (innerClasses == null) {
         innerClasses = new InnerClasses(cp);
      }

      innerClasses.add(name, outerName, innerName, access);
   }

   @Nonnull @Override
   public FieldVisitor visitField(
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable Object value
   ) {
      FieldWriter field = new FieldWriter(this, access, name, desc, signature, value);
      fields.add(field);
      return field;
   }

   @Nonnull @Override
   public MethodWriter visitMethod(
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions
   ) {
      MethodWriter method = new MethodWriter(this, access, name, desc, signature, exceptions, computeFrames);
      methods.add(method);
      return method;
   }

   // ------------------------------------------------------------------------
   // Other public methods
   // ------------------------------------------------------------------------

   /**
    * Returns the bytecode of the class that was build with this class writer.
    */
   @Nonnull @Override
   public byte[] toByteArray() {
      cp.checkConstantPoolMaxSize();

      int size = getBytecodeSize(); // the real size of the bytecode of this class
      int attributeCount = getAttributeCount();

      ByteVector out = putClassAttributes(size, attributeCount);
      putAnnotations(out);
      return out.data;
   }

   @Nonnegative
   private int getBytecodeSize() {
      int size =
         24 +
         getInterfacesSize() + getFieldsSize() + getMethodsSize() +
         bootstrapMethods.getSize() + sourceInfo.getSize();

      if (signature != 0) {
         size += 8;
         cp.newUTF8("Signature");
      }

      if (outerClass != null) {
         size += outerClass.getSize();
      }

      if (Access.isDeprecated(access)) {
         size += 6;
         cp.newUTF8("Deprecated");
      }

      if (isSynthetic()) {
         size += 6;
         cp.newUTF8("Synthetic");
      }

      if (innerClasses != null) {
         size += innerClasses.getSize();
      }

      return size + getAnnotationsSize() + cp.getSize();
   }

   @Nonnegative
   private int getAttributeCount() {
      int attributeCount = 0;

      if (bootstrapMethods.hasMethods()) {
         attributeCount++;
      }

      if (signature != 0) {
         attributeCount++;
      }

      attributeCount += sourceInfo.getAttributeCount();

      if (outerClass != null) {
         attributeCount++;
      }

      if (Access.isDeprecated(access)) {
         attributeCount++;
      }

      if (isSynthetic()) {
         attributeCount++;
      }

      if (innerClasses != null) {
         attributeCount++;
      }

      if (annotations != null) {
         attributeCount++;
      }

      return attributeCount;
   }

   @Nonnegative
   private int getInterfacesSize() {
      return interfaces == null ? 0 : 2 * interfaces.getCount();
   }

   @Nonnegative
   private int getFieldsSize() {
      int size = 0;

      for (FieldWriter fb : fields) {
         size += fb.getSize();
      }

      return size;
   }

   @Nonnegative
   private int getMethodsSize() {
      int size = 0;

      for (MethodWriter mb : methods) {
         size += mb.getSize();
      }

      return size;
   }

   @Nonnull
   private ByteVector putClassAttributes(@Nonnegative int size, @Nonnegative int attributeCount) {
      // Allocates a byte vector of this size, in order to avoid unnecessary arraycopy operations in the
      // ByteVector.enlarge() method.
      ByteVector out = new ByteVector(size);

      out.putInt(0xCAFEBABE).putInt(version);
      cp.put(out);

      int accessFlag = Access.computeFlag(access, 0);
      out.putShort(accessFlag);

      out.putShort(name);
      out.putShort(superName);

      int interfaceCount = interfaces == null ? 0 : interfaces.getCount();
      out.putShort(interfaceCount);

      if (interfaceCount > 0) {
         interfaces.put(out);
      }

      putFields(out);
      putMethods(out);

      out.putShort(attributeCount);
      bootstrapMethods.put(out);

      putSignature(out);

      sourceInfo.put(out);

      if (outerClass != null) {
         outerClass.put(out);
      }

      if (Access.isDeprecated(access)) {
         out.putShort(cp.newUTF8("Deprecated")).putInt(0);
      }

      if (isSynthetic()) {
         out.putShort(cp.newUTF8("Synthetic")).putInt(0);
      }

      if (innerClasses != null) {
         innerClasses.put(out);
      }

      return out;
   }

   private void putFields(@Nonnull ByteVector out) {
      out.putShort(fields.size());

      for (FieldWriter fw : fields) {
         fw.put(out);
      }
   }

   private void putMethods(@Nonnull ByteVector out) {
      out.putShort(methods.size());

      for (MethodWriter mw : methods) {
         mw.put(out);
      }
   }

   private void putSignature(@Nonnull ByteVector out) {
      if (signature != 0) {
         out.putShort(cp.newUTF8("Signature")).putInt(2).putShort(signature);
      }
   }

   // ------------------------------------------------------------------------
   // Utility methods: version, synthetic
   // ------------------------------------------------------------------------

   int getClassVersion() { return version & 0xFFFF; }

   private boolean isSynthetic() { return isSynthetic(access); }

   boolean isSynthetic(int access) {
      return
         Access.isSynthetic(access) &&
         ((access & Access.SYNTHETIC_ATTRIBUTE) != 0 || getClassVersion() < ClassVersion.V1_5);
   }
}

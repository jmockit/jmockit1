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

import mockit.internal.util.*;

/**
 * A {@link ClassVisitor} that generates classes in bytecode form. More precisely this visitor generates a byte array
 * conforming to the Java class file format. It can be used alone, to generate a Java class "from scratch", or with one
 * or more {@link ClassReader} and adapter class visitor to generate a modified class from one or more existing Java
 * classes.
 *
 * @author Eric Bruneton
 */
public final class ClassWriter extends ClassVisitor
{
   /**
    * The class reader from which this class writer was constructed.
    */
   final ClassReader cr;

   /**
    * Minor and major version numbers of the class to be generated.
    */
   private int version;

   final ConstantPoolGeneration constantPool;

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

   private Interfaces interfaces;
   private final SourceInfo sourceInfo;
   private OuterClass outerClass;
   private InnerClasses innerClasses;
   final BootstrapMethods bootstrapMethods;

   /**
    * The fields of this class.
    */
   private final List<FieldWriter> fields;

   /**
    * The methods of this class.
    */
   private final List<MethodWriter> methods;

   /**
    * <tt>true</tt> if the stack map frames must be recomputed from scratch.
    * <p/>
    * If this flag is set, then the calls to the {@link MethodVisitor#visitFrame} method are ignored, and the stack map
    * frames are recomputed from the methods bytecode. The arguments of the {@link MethodVisitor#visitMaxStack} method
    * are also ignored and recomputed from the bytecode. In other words, computeFrames implies computeMaxs.
    */
   private final boolean computeFrames;

   /**
    * Constructs a new {@link ClassWriter} object and enables optimizations for "mostly add" bytecode transformations.
    * These optimizations are the following:
    * <ul>
    * <li>The constant pool from the original class is copied as is in the new class, which saves time.
    * New constant pool entries will be added at the end if necessary, but unused constant pool entries <i>won't be
    * removed</i>.</li>
    * <li>Methods that are not transformed are copied as is in the new class, directly from the original class bytecode
    * (i.e. without emitting visit events for all the method instructions), which saves a <i>lot</i> of time.
    * Untransformed methods are detected by the fact that the {@link ClassReader} receives {@link MethodVisitor}
    * objects that come from a {@link ClassWriter} (and not from any other {@link ClassVisitor} instance).</li>
    * </ul>
    *
    * @param classReader the {@link ClassReader} used to read the original class. It will be used to copy the entire
    *                    constant pool from the original class and also to copy other fragments of original bytecode
    *                    where applicable.
    */
   public ClassWriter(ClassReader classReader) {
      constantPool = new ConstantPoolGeneration();
      sourceInfo = new SourceInfo(this);
      bootstrapMethods = new BootstrapMethods(this);

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
   public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      this.version = version;
      this.access = access;
      this.name = constantPool.newClass(name);
      thisName = name;

      if (signature != null) {
         this.signature = newUTF8(signature);
      }

      this.superName = superName == null ? 0 : constantPool.newClass(superName);

      if (interfaces != null && interfaces.length > 0) {
         this.interfaces = new Interfaces(this, interfaces);
      }

      if (superName != null) {
         ClassLoad.addSuperClass(name, superName);
      }
   }

   @Override
   public void visitSource(String file, String debug) {
      sourceInfo.add(file, debug);
   }

   @Override
   public void visitOuterClass(String owner, String name, String desc) {
      outerClass = new OuterClass(this, owner, name, desc);
   }

   @Override
   public AnnotationVisitor visitAnnotation(String desc) {
      return visitAnnotation(this, desc);
   }

   @Override
   public void visitInnerClass(String name, String outerName, String innerName, int access) {
      if (innerClasses == null) {
         innerClasses = new InnerClasses(this);
      }

      innerClasses.add(name, outerName, innerName, access);
   }

   @Override
   public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
      FieldWriter field = new FieldWriter(this, access, name, desc, signature, value);
      fields.add(field);
      return field;
   }

   @Override
   public MethodWriter visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
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
   @Override
   public byte[] toByteArray() {
      constantPool.checkConstantPoolMaxSize();

      // Computes the real size of the bytecode of this class.
      int interfaceCount = interfaces == null ? 0 : interfaces.getCount();
      int size = 24 + 2 * interfaceCount;

      size += getFieldsSize();
      size += getMethodsSize();

      int attributeCount = 0;

      if (bootstrapMethods.hasMethods()) {
         // We put it as first attribute in order to improve a bit the performance of copyBootstrapMethods.
         attributeCount++;
         size += bootstrapMethods.getSize();
      }

      if (signature != 0) {
         attributeCount++;
         size += 8;
         newUTF8("Signature");
      }

      attributeCount += sourceInfo.getAttributeCount();
      size += sourceInfo.getSize();

      if (outerClass != null) {
         attributeCount++;
         size += outerClass.getSize();
      }

      boolean deprecated = Access.isDeprecated(access);

      if (deprecated) {
         attributeCount++;
         size += 6;
         newUTF8("Deprecated");
      }

      if (isSynthetic()) {
         attributeCount++;
         size += 6;
         newUTF8("Synthetic");
      }

      if (innerClasses != null) {
         attributeCount++;
         size += innerClasses.getSize();
      }

      if (annotations != null) {
         attributeCount++;
         size += getAnnotationsSize(this);
      }

      size += constantPool.getSize();

      // Allocates a byte vector of this size, in order to avoid unnecessary arraycopy operations in the
      // ByteVector.enlarge() method.
      ByteVector out = new ByteVector(size);

      out.putInt(0xCAFEBABE).putInt(version);
      constantPool.put(out);

      int accessFlag = Access.computeFlag(access, 0);
      out.putShort(accessFlag);

      out.putShort(name);
      out.putShort(superName);
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

      if (deprecated) {
         out.putShort(newUTF8("Deprecated")).putInt(0);
      }

      if (isSynthetic()) {
         out.putShort(newUTF8("Synthetic")).putInt(0);
      }

      if (innerClasses != null) {
         innerClasses.put(out);
      }

      putAnnotations(out, this);

      return out.data;
   }

   private int getMethodsSize() {
      int size = 0;

      for (MethodWriter mb : methods) {
         size += mb.getSize();
      }

      return size;
   }

   private int getFieldsSize() {
      int size = 0;

      for (FieldWriter fb : fields) {
         size += fb.getSize();
      }

      return size;
   }

   private void putFields(ByteVector out) {
      out.putShort(fields.size());

      for (FieldWriter fb : fields) {
         fb.put(out);
      }
   }

   private void putMethods(ByteVector out) {
      out.putShort(methods.size());

      for (MethodWriter mb : methods) {
         mb.put(out);
      }
   }

   private void putSignature(ByteVector out) {
      if (signature != 0) {
         out.putShort(newUTF8("Signature")).putInt(2).putShort(signature);
      }
   }

   // ------------------------------------------------------------------------
   // Utility methods: version, constant pool management
   // ------------------------------------------------------------------------

   int getClassVersion() { return version & 0xFFFF; }

   private boolean isSynthetic() { return isSynthetic(access); }

   boolean isSynthetic(int access) {
      return
         Access.isSynthetic(access) &&
         ((access & Access.SYNTHETIC_ATTRIBUTE) != 0 || getClassVersion() < ClassVersion.V1_5);
   }

   Item newConstItem(Object cst) { return constantPool.newConstItem(cst); }
   int newUTF8(String value) { return constantPool.newUTF8(value); }
   Item newClassItem(String value) { return constantPool.newClassItem(value); }
   int newClass(String value) { return constantPool.newClass(value); }
   Item newFieldItem(String owner, String name, String desc) { return constantPool.newFieldItem(owner, name, desc); }
   Item newInteger(int value) { return constantPool.newInteger(value); }
   Item newFloat(float value) { return constantPool.newFloat(value); }
   Item newLong(long value) { return constantPool.newLong(value); }
   Item newDouble(double value) { return constantPool.newDouble(value); }

   /**
    * Adds a name and type to the constant pool of the class being build.
    * Does nothing if the constant pool already contains a similar item.
    *
    * @param name a name.
    * @param desc a type descriptor.
    * @return the index of a new or already existing name and type item.
    */
   int newNameType(String name, String desc) {
      return constantPool.newNameTypeItem(name, desc).index;
   }

   int addType(String type) { return constantPool.addType(type); }
   int addUninitializedType(String type, int offset) { return constantPool.addUninitializedType(type, offset); }
   int getMergedType(int type1, int type2) { return constantPool.getMergedType(type1, type2); }

   /**
    * Adds an invokedynamic reference to the constant pool of the class being built.
    * Does nothing if the constant pool already contains a similar item.
    *
    * @param name    name of the invoked method.
    * @param desc    descriptor of the invoke method.
    * @param bsm     the bootstrap method.
    * @param bsmArgs the bootstrap method constant arguments.
    * @return a new or an already existing invokedynamic type reference item.
    */
   Item newInvokeDynamicItem(String name, String desc, Handle bsm, Object... bsmArgs) {
      return bootstrapMethods.addInvokeDynamicReference(name, desc, bsm, bsmArgs);
   }
}

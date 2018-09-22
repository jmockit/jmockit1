package mockit.asm;

import java.util.*;

import javax.annotation.*;

import mockit.internal.util.*;

/**
 * A {@link ClassVisitor} that generates classes in bytecode form, that is, a byte array conforming to the
 * <a href="https://docs.oracle.com/javase/specs/jvms/se10/html/jvms-4.html">Java class file format</a>.
 * <p/>
 * It can be used alone, to generate a Java class "from scratch", or with one or more {@link ClassReader} and adapter class visitor to
 * generate a modified class from one or more existing Java classes.
 */
public final class ClassWriter extends ClassVisitor
{
   /**
    * The class bytecode from which this class writer will generate a new/modified class.
    */
   @Nonnull final byte[] code;

   /**
    * <tt>true</tt> if the stack map frames must be recomputed from scratch.
    * <p/>
    * If this flag is set, then the stack map frames are recomputed from the methods bytecode. The arguments of the
    * {@link MethodVisitor#visitMaxStack} method are also ignored and recomputed from the bytecode. In other words,
    * computeFrames implies computeMaxs.
    */
   private final boolean computeFrames;

   /**
    * Minor and major version numbers of the class to be generated.
    */
   int classVersion;

   /**
    * The constant pool item that contains the internal name of this class.
    */
   private int nameItemIndex;

   /**
    * The internal name of this class.
    */
   String thisName;

   /**
    * The constant pool item that contains the internal name of the super class of this class.
    */
   @Nonnegative private int superNameItemIndex;

   @Nullable private Interfaces interfaceItems;
   @Nullable private NestHostWriter nestHostWriter;
   @Nullable private SignatureWriter signatureWriter;
   @Nonnull private final SourceInfoWriter sourceInfo;
   @Nullable private InnerClassesWriter innerClassesWriter;
   @Nullable final BootstrapMethods bootstrapMethods;

   /**
    * The fields of this class.
    */
   @Nonnull private final List<FieldWriter> fields;

   /**
    * The methods of this class.
    */
   @Nonnull private final List<MethodWriter> methods;

   /**
    * Initializes a new class writer, applying the following two optimizations that are useful for "mostly add" bytecode transformations:
    * <ul>
    * <li>The constant pool from the original class is copied as is in the new class, which saves time.
    * New constant pool entries will be added at the end if necessary, but unused constant pool entries <i>won't be removed</i>.</li>
    * <li>Methods that are not transformed are copied as is in the new class, directly from the original class bytecode (i.e. without
    * emitting visit events for all the method instructions), which saves a <i>lot</i> of time. Untransformed methods are detected by the
    * fact that the {@link ClassReader} receives {@link MethodVisitor} objects that come from a ClassWriter (and not from any other
    * {@link ClassVisitor} instance).</li>
    * </ul>
    *
    * @param classReader the {@link ClassReader} used to read the original class. It will be used to copy the entire constant pool from the
    *                    original class and also to copy other fragments of original bytecode where applicable.
    */
   public ClassWriter(@Nonnull ClassReader classReader) {
      code = classReader.code;
      classVersion = classReader.getVersion();
      computeFrames = classVersion >= ClassVersion.V1_7;

      cp = new ConstantPoolGeneration();
      sourceInfo = new SourceInfoWriter(cp);

      bootstrapMethods = classReader.positionAtBootstrapMethodsAttribute() ? new BootstrapMethods(cp, classReader) : null;

      new ConstantPoolCopying(classReader, this).copyPool(bootstrapMethods);

      fields = new ArrayList<>();
      methods = new ArrayList<>();
   }

   @Override
   public void visit(int version, int access, @Nonnull String name, @Nonnull ClassInfo additionalInfo) {
      classVersion = version;
      classOrMemberAccess = access;
      nameItemIndex = cp.newClass(name);
      thisName = name;

      createMarkerAttributes(version);

      String superName = additionalInfo.superName;
      superNameItemIndex = superName == null ? 0 : cp.newClass(superName);

      if (additionalInfo.interfaces.length > 0) {
         interfaceItems = new Interfaces(cp, additionalInfo.interfaces);
      }

      if (additionalInfo.signature != null) {
         signatureWriter = new SignatureWriter(cp, additionalInfo.signature);
      }

      sourceInfo.setSourceFileName(additionalInfo.sourceFileName);

      if (additionalInfo.hostClassName != null) {
         nestHostWriter = new NestHostWriter(cp, additionalInfo.hostClassName);
      }

      if (superName != null) {
         ClassLoad.addSuperClass(name, superName);
      }
   }

   @Nonnull @Override
   public AnnotationVisitor visitAnnotation(@Nonnull String desc) {
      return addAnnotation(desc);
   }

   @Override
   public void visitInnerClass(@Nonnull String name, @Nullable String outerName, @Nullable String innerName, int access) {
      if (innerClassesWriter == null) {
         innerClassesWriter = new InnerClassesWriter(cp);
      }

      innerClassesWriter.add(name, outerName, innerName, access);
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

   /**
    * Returns the bytecode of the class that was build with this class writer.
    */
   @Nonnull @Override
   public byte[] toByteArray() {
      cp.checkConstantPoolMaxSize();

      int size = getBytecodeSize(); // the real size of the bytecode of this class

      // Allocates a byte vector of this size, in order to avoid unnecessary arraycopy operations in the ByteVector.enlarge() method.
      ByteVector out = new ByteVector(size);

      putClassAttributes(out);
      putAnnotations(out);
      return out.data;
   }

   @Nonnegative
   private int getBytecodeSize() {
      int size = 24 + getMarkerAttributesSize() + getInterfacesSize() + getFieldsSize() + getMethodsSize() + sourceInfo.getSize();

      if (bootstrapMethods != null) {
         size += bootstrapMethods.getSize();
      }

      size += getAttributeSize(signatureWriter);
      size += getAttributeSize(nestHostWriter);
      size += getAttributeSize(innerClassesWriter);

      return size + getAnnotationsSize() + cp.getSize();
   }

   @Nonnegative
   private int getInterfacesSize() {
      return interfaceItems == null ? 0 : 2 * interfaceItems.getCount();
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

   @Nonnegative
   private static int getAttributeSize(@Nullable AttributeWriter attributeWriter) {
      return attributeWriter == null ? 0 : attributeWriter.getSize();
   }

   private void putClassAttributes(@Nonnull ByteVector out) {
      out.putInt(0xCAFEBABE).putInt(classVersion);
      cp.put(out);

      putAccess(out, 0);
      out.putShort(nameItemIndex);
      out.putShort(superNameItemIndex);

      int interfaceCount = interfaceItems == null ? 0 : interfaceItems.getCount();
      out.putShort(interfaceCount);

      if (interfaceCount > 0) {
         interfaceItems.put(out);
      }

      BaseWriter.put(out, fields);
      BaseWriter.put(out, methods);

      int attributeCount = getAttributeCount();
      out.putShort(attributeCount);

      if (bootstrapMethods != null) {
         bootstrapMethods.put(out);
      }

      if (signatureWriter != null) {
         signatureWriter.put(out);
      }

      if (nestHostWriter != null) {
         nestHostWriter.put(out);
      }

      sourceInfo.put(out);

      putMarkerAttributes(out);

      if (innerClassesWriter != null) {
         innerClassesWriter.put(out);
      }
   }

   @Nonnegative
   private int getAttributeCount() {
      int attributeCount = getMarkerAttributeCount() + sourceInfo.getAttributeCount();

      if (bootstrapMethods != null) {
         attributeCount++;
      }

      if (signatureWriter != null) {
         attributeCount++;
      }

      if (nestHostWriter != null) {
         attributeCount++;
      }

      if (innerClassesWriter != null) {
         attributeCount++;
      }

      if (annotations != null) {
         attributeCount++;
      }

      return attributeCount;
   }

   boolean isJava6OrNewer() { return classVersion >= ClassVersion.V1_6; }
}

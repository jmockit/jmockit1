package mockit.asm.classes;

import javax.annotation.*;

import mockit.asm.*;
import mockit.asm.fields.*;
import mockit.asm.jvmConstants.*;
import mockit.asm.methods.*;

/**
 * A Java class parser to make a {@link ClassVisitor} visit an existing class.
 * <p/>
 * The Java type to be parsed is given in the form of a byte array conforming to the
 * <a href="https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-4.html">Java class file format</a>.
 * For each field and method encountered, the appropriate visit method of a given class visitor is called.
 */
public final class ClassReader extends AnnotatedReader
{
   public interface Flags {
      /**
       * Flag to skip method code. If this flag is set the <code>CODE</code> attribute won't be visited.
       * This can be used, for example, to retrieve annotations for methods and method parameters.
       */
      int SKIP_CODE = 1;

      /**
       * Flag to skip the debug information in the class.
       * If this flag is set the debug information of the class (local variables and line numbers) is not visited.
       */
      int SKIP_DEBUG = 2;

      /**
       * Flag to skip the inner classes of the class being read.
       */
      int SKIP_INNER_CLASSES = 4;
   }

   /**
    * Start index of the class header information (access, name...) in {@link #code}.
    */
   @Nonnegative final int header;

   @Nonnegative private final int version;
   @Nonnull private final ClassInfo classInfo;

   private ClassVisitor cv;
   @Nonnegative public int flags;
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
      version = readShort(6);
      access = readUnsignedShort();
      classInfo = new ClassInfo();
      codeIndex += 2;
      classInfo.superName = readClass();
   }

   /**
    * Returns the classfile {@linkplain ClassVersion version} of the class being read.
    */
   public int getVersion() { return version; }

   /**
    * Returns the class's {@linkplain Access access} flags.
    */
   public int getAccess() { return access; }

   /**
    * Returns the internal of name of the super class. For interfaces, the super class is {@link Object}.
    */
   @Nonnull
   public String getSuperName() {
      assert classInfo.superName != null;
      return classInfo.superName;
   }

   /**
    * Returns the bytecode array of the Java classfile that was read.
    */
   @Nonnull
   public byte[] getBytecode() { return code; }

   /**
    * Makes the given visitor visit the Java class of this Class Reader, all attributes included.
    */
   public void accept(ClassVisitor visitor) {
      accept(visitor, 0);
   }

   /**
    * Makes the given visitor visit the Java class of this Class Reader.
    *
    * @param optionFlags option {@linkplain Flags flags} that can be used to modify the default behavior of this class
    */
   public void accept(@Nonnull ClassVisitor visitor, @Nonnegative int optionFlags) {
      cv = visitor;
      flags = optionFlags;

      codeIndex = header + 2;
      String classDesc = readNonnullClass();
      codeIndex += 2;

      readInterfaces();
      readClassAttributes();
      visitor.visit(version, access, classDesc, classInfo);
      readAnnotations(visitor);
      readInnerClasses();
      readFieldsAndMethods();

      visitor.visitEnd();
   }

   private void readInterfaces() {
      int interfaceCount = readUnsignedShort();

      if (interfaceCount > 0) {
         String[] interfaces = new String[interfaceCount];

         for (int i = 0; i < interfaceCount; i++) {
            interfaces[i] = readNonnullClass();
         }

         classInfo.interfaces = interfaces;
      }
   }

   private void readClassAttributes() {
      innerClassesCodeIndex = 0;
      codeIndex = getAttributesStartIndex();
      readAttributes();
      classInfo.signature = signature;
   }

   @Nullable @Override
   protected Boolean readAttribute(@Nonnull String attributeName) {
      if ("SourceFile".equals(attributeName)) {
         classInfo.sourceFileName = readNonnullUTF8();
         return true;
      }

      if ("EnclosingMethod".equals(attributeName)) {
         return false;
      }

      if ("NestHost".equals(attributeName)) {
         classInfo.hostClassName = readNonnullClass();
         return true;
      }

      if ("NestMembers".equals(attributeName)) {
         readNestMembers();
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

   private void readNestMembers() {
      int numberOfClasses = readUnsignedShort();
      String[] nestMembers = new String[numberOfClasses];

      for (int i = 0; i < numberOfClasses; i++) {
         nestMembers[i] = readNonnullClass();
      }

      classInfo.nestMembers = nestMembers;
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

   private void readInnerClasses() {
      int startIndex = innerClassesCodeIndex;

      if (startIndex != 0) {
         codeIndex = startIndex;

         for (int innerClassCount = readUnsignedShort(); innerClassCount > 0; innerClassCount--) {
            String innerName = readNonnullClass();
            String outerName = readClass();
            String simpleInnerName = readUTF8();
            int innerAccess = readUnsignedShort();

            cv.visitInnerClass(innerName, outerName, simpleInnerName, innerAccess);
         }
      }
   }

   private void readFieldsAndMethods() {
      codeIndex = getCodeIndexAfterInterfaces(classInfo.interfaces.length);

      FieldReader fieldReader = new FieldReader(this, cv);
      codeIndex = fieldReader.readFields();

      MethodReader methodReader = new MethodReader(this, cv);
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

   @Nonnegative
   public int getBSMCodeIndex(@Nonnegative int bsmStartIndex) {
      assert bootstrapMethods != null;
      return bootstrapMethods[bsmStartIndex];
   }
}

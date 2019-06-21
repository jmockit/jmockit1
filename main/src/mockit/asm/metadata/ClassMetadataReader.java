/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.asm.metadata;

import java.nio.charset.*;
import java.util.*;
import javax.annotation.*;

import mockit.asm.jvmConstants.*;

public final class ClassMetadataReader extends ObjectWithAttributes
{
   private static final Charset UTF8 = Charset.forName("UTF-8");
   private static final ConstantPoolTag[] CONSTANT_POOL_TAGS = ConstantPoolTag.values();

   enum ConstantPoolTag { // values from JVM spec Table 4.4.A
      No0,                   // 0
      Utf8(2),               // 1 (has variable size)
      No2,                   // 2
      Integer(4),            // 3
      Float(4),              // 4
      Long(8),               // 5
      Double(8),             // 6
      Class(2),              // 7
      String(2),             // 8
      FieldRef(4),           // 9
      MethodRef(4),          // 10
      InterfaceMethodRef(4), // 11
      NameAndType(4),        // 12
      No13,
      No14,
      MethodHandle(3),       // 15, added in Java 7
      MethodType(2),         // 16, added in Java 7
      ConstantDynamic(4),    // 17, added in Java 11
      InvokeDynamic(4),      // 18, added in Java 7
      Module(2),             // 19, added in Java 9
      Package(2);            // 20, added in Java 9

      @Nonnegative final int itemSize;
      ConstantPoolTag() { itemSize = 0; }
      ConstantPoolTag(@Nonnegative int itemSize) { this.itemSize = itemSize; }
   }

   public enum Attribute {
      Annotations,
      Parameters,
      Signature
   }

   @Nonnull private final byte[] code;
   @Nonnull private final int[] cpItemCodeIndexes;
   @Nullable private final EnumSet<Attribute> attributesToRead;

   /**
    * The constant pool starts at index 10 in the code array; this is the end index, which must be computed as it's not stored anywhere.
    */
   @Nonnegative private final int cpEndIndex;

   @Nonnegative private int fieldsEndIndex;
   @Nonnegative private int methodsEndIndex;

   @Nonnegative
   public static int readVersion(@Nonnull byte[] code) {
      int byte0 = (code[4] & 0xFF) << 24;
      int byte1 = (code[5] & 0xFF) << 16;
      int byte2 = (code[6] & 0xFF) << 8;
      int byte3 =  code[7] & 0xFF;
      return byte0 | byte1 | byte2 | byte3;
   }

   public ClassMetadataReader(@Nonnull byte[] code) { this(code, null); }

   public ClassMetadataReader(@Nonnull byte[] code, @Nullable EnumSet<Attribute> attributesToRead) {
      this.code = code;
      int cpItemCount = readUnsignedShort(8);
      int[] cpTable = new int[cpItemCount];
      cpItemCodeIndexes = cpTable;
      this.attributesToRead = attributesToRead;
      cpEndIndex = findEndIndexOfConstantPoolTable(cpTable);
   }

   @Nonnegative
   private int readUnsignedShort(@Nonnegative int codeIndex) {
      byte[] b = code;
      int i = codeIndex;
      int byte0 = (b[i++] & 0xFF) << 8;
      int byte1 =  b[i] & 0xFF;
      return byte0 | byte1;
   }

   private int readInt(@Nonnegative int codeIndex) {
      byte[] b = code;
      int i = codeIndex;
      int byte0 = (b[i++] & 0xFF) << 24;
      int byte1 = (b[i++] & 0xFF) << 16;
      int byte2 = (b[i++] & 0xFF) << 8;
      int byte3 =  b[i] & 0xFF;
      return byte0 | byte1 | byte2 | byte3;
   }

   @Nonnegative
   private int findEndIndexOfConstantPoolTable(@Nonnull int[] cpTable) {
      byte[] b = code;
      int codeIndex = 10;

      for (int cpItemIndex = 1, n = cpTable.length; cpItemIndex < n; cpItemIndex++) {
         int tagValue = b[codeIndex++];
         ConstantPoolTag tag = CONSTANT_POOL_TAGS[tagValue];

         cpTable[cpItemIndex] = codeIndex;

         int cpItemSize = tag.itemSize;

         if (tag == ConstantPoolTag.Long || tag == ConstantPoolTag.Double) {
            cpItemIndex++;
         }
         else if (tag == ConstantPoolTag.Utf8) {
            int stringLength = readUnsignedShort(codeIndex);
            cpItemSize += stringLength;
         }

         codeIndex += cpItemSize;
      }

      return codeIndex;
   }

   @Nonnegative
   public int getVersion() {
      return readVersion(code);
   }

   @Nonnegative
   public int getAccessFlags() {
      return readUnsignedShort(cpEndIndex);
   }

   @Nonnull
   public String getThisClass() {
      int cpClassIndex = readUnsignedShort(cpEndIndex + 2);
      return getTypeDescription(cpClassIndex);
   }

   @Nonnull
   private String getTypeDescription(@Nonnegative int cpClassIndex) {
      int cpClassCodeIndex = cpItemCodeIndexes[cpClassIndex];
      int cpDescriptionIndex = readUnsignedShort(cpClassCodeIndex);
      return getString(cpDescriptionIndex);
   }

   @Nonnull
   private String getString(@Nonnegative int cpStringIndex) {
      int codeIndex = cpItemCodeIndexes[cpStringIndex];
      int stringLength = readUnsignedShort(codeIndex);
      return new String(code, codeIndex + 2, stringLength, UTF8);
   }

   @Nullable
   public String getSuperClass() {
      int cpClassIndex = readUnsignedShort(cpEndIndex + 4);

      if (cpClassIndex == 0) {
         return null;
      }

      return getTypeDescription(cpClassIndex);
   }

   @Nullable
   public String[] getInterfaces() {
      int codeIndex = cpEndIndex + 6;
      int interfaceCount = readUnsignedShort(codeIndex);

      if (interfaceCount == 0) {
         return null;
      }

      codeIndex += 2;

      String[] interfaces = new String[interfaceCount];

      for (int i = 0; i < interfaceCount; i++) {
         int cpInterfaceIndex = readUnsignedShort(codeIndex);
         codeIndex += 2;
         interfaces[i] = getTypeDescription(cpInterfaceIndex);
      }

      return interfaces;
   }

   private static class MemberInfo extends ObjectWithAttributes {
      @Nonnegative public final int accessFlags;
      @Nonnull public final String name;
      @Nonnull public final String desc;
      @Nullable public String signature;

      MemberInfo(@Nonnegative int accessFlags, @Nonnull String name, @Nonnull String desc, @Nonnegative int attributeCount) {
         this.accessFlags = accessFlags;
         this.name = name;
         this.desc = desc;
      }

      public final boolean isStatic()    { return (accessFlags & Access.STATIC) != 0; }
      public final boolean isAbstract()  { return (accessFlags & Access.ABSTRACT) != 0; }
      public final boolean isSynthetic() { return (accessFlags & Access.SYNTHETIC) != 0; }
   }

   public static final class FieldInfo extends MemberInfo {
      FieldInfo(int accessFlags, @Nonnull String name, @Nonnull String desc, @Nonnegative int attributeCount) {
         super(accessFlags, name, desc, attributeCount);
      }
   }

   @Nonnull
   public List<FieldInfo> getFields() {
      int codeIndex = cpEndIndex + 6;
      int interfaceCount = readUnsignedShort(codeIndex);
      codeIndex += 2 + 2 * interfaceCount;

      int fieldCount = readUnsignedShort(codeIndex);
      codeIndex += 2;

      List<FieldInfo> fields;

      if (fieldCount == 0) {
         fields = Collections.emptyList();
      }
      else {
         fields = new ArrayList<>(fieldCount);

         for (int i = 0; i < fieldCount; i++) {
            int accessFlags = readUnsignedShort(codeIndex);
            codeIndex += 2;

            int cpNameIndex = readUnsignedShort(codeIndex);
            codeIndex += 2;
            String fieldName = getString(cpNameIndex);

            int cpDescIndex = readUnsignedShort(codeIndex);
            codeIndex += 2;
            String fieldDesc = getString(cpDescIndex);

            int attributeCount = readUnsignedShort(codeIndex);
            codeIndex += 2;

            FieldInfo fieldInfo = new FieldInfo(accessFlags, fieldName, fieldDesc, attributeCount);
            codeIndex = readAttributes(attributeCount, fieldInfo, codeIndex);
            fields.add(fieldInfo);
         }
      }

      fieldsEndIndex = codeIndex;
      return fields;
   }

   @Nonnegative
   private int readAttributes(@Nonnegative int attributeCount, @Nullable ObjectWithAttributes attributeOwner, @Nonnegative int codeIndex) {
      EnumSet<Attribute> attributes = attributesToRead;
      boolean readAnnotations = false;

      if (attributes == null) {
         //noinspection AssignmentToMethodParameter
         attributeOwner = null;
      }
      else {
         readAnnotations = attributes.contains(Attribute.Annotations);
      }

      MethodInfo method = attributeOwner instanceof MethodInfo ? (MethodInfo) attributeOwner : null;

      for (int i = 0; i < attributeCount; i++) {
         int cpNameIndex = readUnsignedShort(codeIndex);
         codeIndex += 2;
         String attributeName = getString(cpNameIndex);

         int attributeLength = readInt(codeIndex);
         codeIndex += 4;

         if (attributeOwner != null) {
            if (method != null) {
               method.readAttributes(attributeName, codeIndex);
            }

            if (readAnnotations && "RuntimeVisibleAnnotations".equals(attributeName)) {
               attributeOwner.annotations = readAnnotations(codeIndex);
            }
         }

         codeIndex += attributeLength;
      }

      return codeIndex;
   }

   public static final class AnnotationInfo {
      @Nonnull public final String name;

      AnnotationInfo(@Nonnull String name) { this.name = name; }
   }

   @Nonnull
   private List<AnnotationInfo> readAnnotations(@Nonnegative int codeIndex) {
      int numAnnotations = readUnsignedShort(codeIndex);
      codeIndex += 2;

      List<AnnotationInfo> annotationInfos = new ArrayList<>(numAnnotations);

      for (int i = 0; i < numAnnotations; i++) {
         codeIndex = readAnnotation(annotationInfos, codeIndex);
      }

      return annotationInfos;
   }

   @Nonnegative
   private int readAnnotation(@Nonnull List<AnnotationInfo> currentAnnotations, @Nonnegative int codeIndex) {
      int cpTypeIndex = readUnsignedShort(codeIndex);
      codeIndex += 2;

      String annotationTypeDesc = getString(cpTypeIndex);

      int numElementValuePairs = readUnsignedShort(codeIndex);
      codeIndex += 2;

//      for (int i = 0; i < numElementValuePairs; i++) {
//         int cpElementNameIndex = readUnsignedShort(codeIndex);
//         codeIndex += 2;
//
//         int tag = code[codeIndex++];
//         // TODO: continue implementing
//      }

      AnnotationInfo annotation = new AnnotationInfo(annotationTypeDesc);
      currentAnnotations.add(annotation);

      return codeIndex;
   }

   @Nonnegative
   private int getFieldsEndIndex() {
      int codeIndex = fieldsEndIndex;

      if (codeIndex == 0) {
         codeIndex = cpEndIndex + 6;
         int interfaceCount = readUnsignedShort(codeIndex);
         codeIndex += 2 + 2 * interfaceCount;

         int fieldCount = readUnsignedShort(codeIndex);
         codeIndex += 2;

         for (int i = 0; i < fieldCount; i++) {
            codeIndex += 6;

            int attributeCount = readUnsignedShort(codeIndex);
            codeIndex += 2;

            codeIndex = readAttributes(attributeCount, null, codeIndex);
         }

         fieldsEndIndex = codeIndex;
      }

      return codeIndex;
   }

   public final class MethodInfo extends MemberInfo {
      @Nullable public String[] parameters;

      MethodInfo(int accessFlags, @Nonnull String name, @Nonnull String desc, @Nonnegative int attributeCount) {
         super(accessFlags, name, desc, attributeCount);
      }

      public boolean isMethod() { return name.charAt(0) != '<'; }
      public boolean isConstructor() { return "<init>".equals(name); }

      void readAttributes(@Nonnull String attributeName, @Nonnegative int codeIndex) {
         assert attributesToRead != null;

         if ("Code".equals(attributeName)) {
            if (attributesToRead.contains(Attribute.Parameters)) {
               readParameters(codeIndex);
            }
         }
         else if ("Signature".equals(attributeName)) {
            if (attributesToRead.contains(Attribute.Signature)) {
               readSignature(codeIndex);
            }
         }
      }

      private void readParameters(@Nonnegative int codeIndex) {
         codeIndex += 4;

         int codeLength = readInt(codeIndex);
         codeIndex += 4 + codeLength;

         int exceptionTableLength = readUnsignedShort(codeIndex);
         codeIndex += 2 + 8 * exceptionTableLength;

         int attributeCount = readUnsignedShort(codeIndex);
         codeIndex += 2;

         readParameters(attributeCount, codeIndex);
      }

      private void readParameters(@Nonnegative int attributeCount, @Nonnegative int codeIndex) {
         for (int i = 0; i < attributeCount; i++) {
            int cpNameIndex = readUnsignedShort(codeIndex);
            codeIndex += 2;
            String attributeName = getString(cpNameIndex);

            int attributeLength = readInt(codeIndex);
            codeIndex += 4;

            if ("LocalVariableTable".equals(attributeName)) {
               parameters = readParametersFromLocalVariableTable(codeIndex);
               break;
            }

            codeIndex += attributeLength;
         }
      }

      @Nullable
      private String[] readParametersFromLocalVariableTable(@Nonnegative int codeIndex) {
         int localVariableTableLength = readUnsignedShort(codeIndex);
         codeIndex += 2;

         int arraySize = getSumOfArgumentSizes(desc);

         if (arraySize == 0) {
            return null;
         }

         if (!isStatic()) {
            arraySize++;
         }

         String[] parameterNames = new String[arraySize];

         for (int i = 0; i < localVariableTableLength; i++) {
            codeIndex += 4;

            int cpLocalVarNameIndex = readUnsignedShort(codeIndex);
            codeIndex += 2;
            String localVarName = getString(cpLocalVarNameIndex);

            if ("this".equals(localVarName)) {
               codeIndex += 4;
               continue;
            }

            codeIndex += 2;

            int localVarIndex = readUnsignedShort(codeIndex);
            codeIndex += 2;

            if (localVarIndex < arraySize) {
               parameterNames[localVarIndex] = localVarName;
            }
         }

         return compactArray(parameterNames);
      }

      @Nonnegative
      private int getSumOfArgumentSizes(@Nonnull String memberDesc) {
         int sum = 0;
         int i = 1;

         while (true) {
            char c = memberDesc.charAt(i);
            i++;

            if (c == ')') {
               return sum;
            }

            if (c == 'L') {
               while (memberDesc.charAt(i) != ';') i++;
               i++;
               sum++;
            }
            else if (c == '[') {
               while ((c = memberDesc.charAt(i)) == '[') i++;

               if (isDoubleSizeType(c)) { // if the array element type is double size...
                  i++;
                  sum++; // ...then count it here, otherwise let the outer loop count it
               }
            }
            else if (isDoubleSizeType(c)) {
               sum += 2;
            }
            else {
               sum++;
            }
         }
      }

      private boolean isDoubleSizeType(char typeCode) { return typeCode == 'D' || typeCode == 'J'; }

      @Nullable
      private String[] compactArray(@Nonnull String[] arrayPossiblyWithNulls) {
         int n = arrayPossiblyWithNulls.length;
         int j = n - 1;
         int i = 0;

         while (i < j) {
            if (arrayPossiblyWithNulls[i] == null) {
               System.arraycopy(arrayPossiblyWithNulls, i + 1, arrayPossiblyWithNulls, i, j - i);
               arrayPossiblyWithNulls[j] = null;
               j--;
            }
            else {
               i++;
            }
         }

         return n == 1 && arrayPossiblyWithNulls[0] == null ? null : arrayPossiblyWithNulls;
      }

      private void readSignature(@Nonnegative int codeIndex) {
         int cpSignatureIndex = readUnsignedShort(codeIndex);
         signature = getString(cpSignatureIndex);
      }
   }

   @Nonnull
   public List<MethodInfo> getMethods() {
      int codeIndex = getFieldsEndIndex();
      int methodCount = readUnsignedShort(codeIndex);
      codeIndex += 2;

      List<MethodInfo> methods = new ArrayList<>(methodCount);

      for (int i = 0; i < methodCount; i++) {
         int accessFlags = readUnsignedShort(codeIndex);
         codeIndex += 2;

         int cpNameIndex = readUnsignedShort(codeIndex);
         codeIndex += 2;
         String methodName = getString(cpNameIndex);

         int cpDescIndex = readUnsignedShort(codeIndex);
         codeIndex += 2;
         String methodDesc = getString(cpDescIndex);

         int attributeCount = readUnsignedShort(codeIndex);
         codeIndex += 2;

         MethodInfo methodInfo = new MethodInfo(accessFlags, methodName, methodDesc, attributeCount);
         codeIndex = readAttributes(attributeCount, methodInfo, codeIndex);
         methods.add(methodInfo);
      }

      methodsEndIndex = codeIndex;
      return methods;
   }

   @Nonnegative
   private int getMethodsEndIndex() {
      int codeIndex = methodsEndIndex;

      if (codeIndex == 0) {
         codeIndex = getFieldsEndIndex();

         int methodCount = readUnsignedShort(codeIndex);
         codeIndex += 2;

         for (int i = 0; i < methodCount; i++) {
            codeIndex += 6;

            int attributeCount = readUnsignedShort(codeIndex);
            codeIndex += 2;

            codeIndex = readAttributes(attributeCount, null, codeIndex);
         }

         methodsEndIndex = codeIndex;
      }

      return codeIndex;
   }

   @Nonnull
   public List<AnnotationInfo> getAnnotations() {
      if (annotations == null) {
         int codeIndex = getMethodsEndIndex();
         int attributeCount = readUnsignedShort(codeIndex);
         codeIndex += 2;

         readAttributes(attributeCount, this, codeIndex);

         if (annotations == null) {
            annotations = Collections.emptyList();
         }
      }

      return annotations;
   }
}

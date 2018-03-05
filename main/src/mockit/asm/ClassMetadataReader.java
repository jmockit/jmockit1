/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.asm;

import java.nio.charset.*;
import java.util.*;
import javax.annotation.*;

public final class ClassMetadataReader
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

      // Added in Java 7:
      MethodHandle(3),       // 15
      MethodType(2),         // 16
      No17,
      InvokeDynamic(4),      // 18

      // Added in Java 9:
      Module(2),             // 19
      Package(2);            // 20

      @Nonnegative final int itemSize;
      ConstantPoolTag() { itemSize = 0; }
      ConstantPoolTag(@Nonnegative int itemSize) { this.itemSize = itemSize; }
   }

   @Nonnull private final byte[] code;
   @Nonnull private final int[] cpItemCodeIndexes;

   /**
    * The constant pool starts at index 10 in the code array; this is the end index, which must be computed as it's not stored anywhere.
    */
   @Nonnegative private final int cpEndIndex;

   @Nonnegative private int fieldsEndIndex;
   @Nonnegative private int methodsEndIndex;

   public ClassMetadataReader(@Nonnull byte[] code) {
      this.code = code;
      int cpItemCount = readUnsignedShort(8);
      int[] cpTable = new int[cpItemCount];
      this.cpItemCodeIndexes = cpTable;
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
      int byte3 =  b[i++] & 0xFF;
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
      return readInt(4);
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

   public static final class AttributeInfo {
      public final String name;

      AttributeInfo(@Nonnull String name) { this.name = name; }
   }

   private static class MemberInfo {
      public final int accessFlags;
      public final String name;
      public final String desc;
      public final List<AttributeInfo> attributes;

      MemberInfo(int accessFlags, @Nonnull String name, @Nonnull String desc, @Nonnegative int attributeCount) {
         this.accessFlags = accessFlags;
         this.name = name;
         this.desc = desc;
         attributes = attributeCount == 0 ? Collections.<AttributeInfo>emptyList() : new ArrayList<AttributeInfo>(attributeCount);
      }
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
         fields = new ArrayList<FieldInfo>(fieldCount);

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
            codeIndex = readAttributes(attributeCount, fieldInfo.attributes, codeIndex);
            fields.add(fieldInfo);
         }
      }

      fieldsEndIndex = codeIndex;
      return fields;
   }

   @Nonnegative
   private int readAttributes(@Nonnegative int attributeCount, @Nullable List<AttributeInfo> attributes, @Nonnegative int codeIndex) {
      for (int i = 0; i < attributeCount; i++) {
         int cpNameIndex = readUnsignedShort(codeIndex);
         codeIndex += 2;
         String attributeName = getString(cpNameIndex);

         int attributeLength = readInt(codeIndex);
         codeIndex += 4;

         if (attributes != null) {
            attributes.add(new AttributeInfo(attributeName));
         }

         codeIndex += attributeLength;
      }

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

   public static final class MethodInfo extends MemberInfo {
      MethodInfo(int accessFlags, @Nonnull String name, @Nonnull String desc, @Nonnegative int attributeCount) {
         super(accessFlags, name, desc, attributeCount);
      }
   }

   @Nonnull
   public List<MethodInfo> getMethods() {
      int codeIndex = getFieldsEndIndex();
      int methodCount = readUnsignedShort(codeIndex);
      codeIndex += 2;

      List<MethodInfo> methods = new ArrayList<MethodInfo>(methodCount);

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
         codeIndex = readAttributes(attributeCount, methodInfo.attributes, codeIndex);
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
   public List<AttributeInfo> getAttributes() {
      int codeIndex = getMethodsEndIndex();
      int attributeCount = readUnsignedShort(codeIndex);
      codeIndex += 2;

      List<AttributeInfo> attributes = new ArrayList<AttributeInfo>(attributeCount);
      readAttributes(attributeCount, attributes, codeIndex);

      return attributes;
   }
}

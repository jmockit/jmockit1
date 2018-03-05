/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.asm;

import java.nio.charset.*;
import javax.annotation.*;

public final class DirectClassReader
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

      @Nonnegative private final int itemSize;
      ConstantPoolTag() { itemSize = 0; }
      ConstantPoolTag(@Nonnegative int itemSize) { this.itemSize = itemSize; }
   }

   @Nonnull private final byte[] code;
   @Nonnull private final int[] cpItemCodeIndexes;

   /**
    * The constant pool starts at index 10 in the code array; this is the end index, which must be computed as it's not stored anywhere.
    */
   @Nonnegative private final int cpEndIndex;

   public DirectClassReader(@Nonnull byte[] code) {
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

   @Nullable
   public String getSuperClass() {
      int cpClassIndex = readUnsignedShort(cpEndIndex + 4);

      if (cpClassIndex == 0) {
         return null;
      }

      return getString(cpClassIndex);
   }

   @Nonnull
   private String getString(@Nonnegative int cpItemIndex) {
      int[] codeIndexes = cpItemCodeIndexes;

      int cpItemCodeIndex = codeIndexes[cpItemIndex];
      int cpNameIndex = readUnsignedShort(cpItemCodeIndex);

      int nameCodeIndex = codeIndexes[cpNameIndex];
      int nameLength = readUnsignedShort(nameCodeIndex);

      return new String(code, nameCodeIndex + 2, nameLength, UTF8);
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
         interfaces[i] = getString(cpInterfaceIndex);
      }

      return interfaces;
   }
}

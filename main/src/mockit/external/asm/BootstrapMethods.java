package mockit.external.asm;

import javax.annotation.*;

/**
 * Generates the "BootstrapMethods" attribute in a class file being written by a {@link ClassWriter}.
 */
final class BootstrapMethods
{
   @Nonnull private final ConstantPoolGeneration constantPool;

   /**
    * The number of entries in the BootstrapMethods attribute.
    */
   @Nonnegative private int bootstrapMethodsCount;

   /**
    * The BootstrapMethods attribute.
    */
   @Nullable private ByteVector bootstrapMethods;

   BootstrapMethods(@Nonnull ConstantPoolGeneration constantPool) { this.constantPool = constantPool; }

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
   @Nonnull
   InvokeDynamicItem addInvokeDynamicReference(
      @Nonnull String name, @Nonnull String desc, @Nonnull Handle bsm, @Nonnull Object... bsmArgs
   ) {
      ByteVector methods = bootstrapMethods;

      if (methods == null) {
         methods = bootstrapMethods = new ByteVector();
      }

      int position = methods.length; // record current position

      int hashCode = bsm.hashCode();
      HandleItem handleItem = constantPool.newHandleItem(bsm);
      methods.putShort(handleItem.index);

      int argsLength = bsmArgs.length;
      methods.putShort(argsLength);

      hashCode = putBSMArgs(methods, hashCode, bsmArgs);

      byte[] data = methods.data;
      int length = (1 + 1 + argsLength) << 1; // (bsm + argCount + arguments)
      hashCode &= 0x7FFFFFFF;

      BootstrapMethodItem bsmItem = getBSMItem(position, hashCode, data, length);
      int bsmIndex;

      if (bsmItem != null) {
         bsmIndex = bsmItem.index;
         methods.length = position; // revert to old position
      }
      else {
         bsmIndex = bootstrapMethodsCount++;
         bsmItem = new BootstrapMethodItem(bsmIndex, position, hashCode);
         constantPool.put(bsmItem);
      }

      // Now, create the InvokeDynamic constant.
      InvokeDynamicItem result = constantPool.createInvokeDynamicItem(name, desc, bsmIndex);
      return result;
   }

   private int putBSMArgs(@Nonnull ByteVector methods, int hashCode, @Nonnull Object[] bsmArgs) {
      for (Object bsmArg : bsmArgs) {
         hashCode ^= bsmArg.hashCode();

         Item constItem = constantPool.newConstItem(bsmArg);
         methods.putShort(constItem.index);
      }

      return hashCode;
   }

   @Nullable @SuppressWarnings("MethodWithMultipleLoops")
   private BootstrapMethodItem getBSMItem(
      @Nonnegative int position, int hashCode, @Nonnull byte[] data, @Nonnegative int length
   ) {
      Item item = constantPool.getItem(hashCode);
      BootstrapMethodItem bsmItem = null;

   loop:
      while (item != null) {
         if (!(item instanceof BootstrapMethodItem) || item.hashCode != hashCode) {
            item = item.next;
            continue;
         }

         bsmItem = (BootstrapMethodItem) item;

         // Because the data encode the size of the argument we don't need to test if these sizes are equal.
         int resultPosition = bsmItem.position;

         for (int p = 0; p < length; p++) {
            if (data[position + p] != data[resultPosition + p]) {
               item = item.next;
               bsmItem = null;
               continue loop;
            }
         }

         break;
      }

      return bsmItem;
   }

   boolean hasMethods() { return bootstrapMethods != null; }

   @Nonnegative
   int getSize() {
      if (bootstrapMethods == null) {
         return 0;
      }

      constantPool.newUTF8("BootstrapMethods");
      return 8 + bootstrapMethods.length;
   }

   void put(@Nonnull ByteVector out) {
      if (hasMethods()) {
         out.putShort(constantPool.newUTF8("BootstrapMethods"));
         //noinspection ConstantConditions
         out.putInt(bootstrapMethods.length + 2).putShort(bootstrapMethodsCount);
         out.putByteVector(bootstrapMethods);
      }
   }

   /**
    * Copies the bootstrap method data from the given {@link ClassReader}.
    */
   void copyBootstrapMethods(@Nonnull ClassReader cr, @Nonnull Item[] items) {
      int codeIndex = findBootstrapMethodsAttribute(cr);

      if (codeIndex == 0) {
         return;
      }

      int bsmCount = cr.readUnsignedShort(codeIndex + 8);
      int bsmCodeStartIndex = codeIndex + 10;

      for (int bsmIndex = 0, bsmCodeIndex = bsmCodeStartIndex; bsmIndex < bsmCount; bsmIndex++) {
         int position = bsmCodeIndex - bsmCodeStartIndex;
         bsmCodeIndex = copyBootstrapMethod(cr, items, bsmIndex, bsmCodeIndex, position);
      }

      int attrSize = cr.readInt(codeIndex + 4);
      bootstrapMethods = new ByteVector(attrSize + 62);
      bootstrapMethods.putByteArray(cr.code, bsmCodeStartIndex, attrSize - 2);
      bootstrapMethodsCount = bsmCount;
   }

   @Nonnegative
   private static int copyBootstrapMethod(
      @Nonnull ClassReader cr, @Nonnull Item[] items, @Nonnegative int bsmIndex, @Nonnegative int bsmCodeIndex,
      @Nonnegative int position
   ) {
      int hashCode = cr.readConstItem(bsmCodeIndex).hashCode();
      bsmCodeIndex += 2;

      int bsmConstCount = cr.readUnsignedShort(bsmCodeIndex);

      for (int bsmConstIndex = bsmConstCount; bsmConstIndex > 0; bsmConstIndex--) {
         Object aConst = cr.readConstItem(bsmCodeIndex + 2);
         hashCode ^= aConst.hashCode();
         bsmCodeIndex += 2;
      }

      bsmCodeIndex += 2;

      BootstrapMethodItem item = new BootstrapMethodItem(bsmIndex, position, hashCode);
      item.setNext(items);

      return bsmCodeIndex;
   }

   @Nonnegative
   private static int findBootstrapMethodsAttribute(@Nonnull ClassReader cr) {
      int codeIndex = cr.getAttributesStartIndex();

      for (int attributeCount = cr.readUnsignedShort(codeIndex); attributeCount > 0; attributeCount--) {
         String attrName = cr.readNonnullUTF8(codeIndex + 2);

         if ("BootstrapMethods".equals(attrName)) {
            return codeIndex;
         }

         codeIndex += 4;
         int codeOffset = cr.readInt(codeIndex);
         codeIndex += 2 + codeOffset;
      }

      return 0;
   }
}

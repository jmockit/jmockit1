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
   Item addInvokeDynamicReference(
      @Nonnull String name, @Nonnull String desc, @Nonnull Handle bsm, @Nonnull Object... bsmArgs
   ) {
      ByteVector methods = bootstrapMethods;

      if (methods == null) {
         methods = bootstrapMethods = new ByteVector();
      }

      int position = methods.length; // record current position

      int hashCode = bsm.hashCode();
      Item handleItem = constantPool.newHandleItem(bsm);
      methods.putShort(handleItem.index);

      int argsLength = bsmArgs.length;
      methods.putShort(argsLength);

      hashCode = putBSMArgs(hashCode, bsmArgs);

      byte[] data = methods.data;
      int length = (1 + 1 + argsLength) << 1; // (bsm + argCount + arguments)
      hashCode &= 0x7FFFFFFF;

      Item bsmItem = getBSMItem(position, hashCode, data, length);
      int bsmIndex;

      if (bsmItem != null) {
         bsmIndex = bsmItem.index;
         methods.length = position; // revert to old position
      }
      else {
         bsmIndex = bootstrapMethodsCount++;
         bsmItem = new Item(bsmIndex);
         bsmItem.set(position, hashCode);
         constantPool.put(bsmItem);
      }

      // Now, create the InvokeDynamic constant.
      Item result = constantPool.createInvokeDynamicConstant(name, desc, bsmIndex);
      return result;
   }

   private int putBSMArgs(int hashCode, @Nonnull Object[] bsmArgs) {
      @SuppressWarnings("ConstantConditions") @Nonnull ByteVector methods = bootstrapMethods;

      for (int i = 0; i < bsmArgs.length; i++) {
         Object bsmArg = bsmArgs[i];
         hashCode ^= bsmArg.hashCode();

         Item constItem = constantPool.newConstItem(bsmArg);
         methods.putShort(constItem.index);
      }

      return hashCode;
   }

   @Nullable
   private Item getBSMItem(@Nonnegative int position, int hashCode, @Nonnull byte[] data, @Nonnegative int length) {
      Item bsmItem = constantPool.getItem(hashCode);

   loop:
      while (bsmItem != null) {
         if (bsmItem.type != ConstantPoolItemType.BSM || bsmItem.hashCode != hashCode) {
            bsmItem = bsmItem.next;
            continue;
         }

         // Because the data encode the size of the argument we don't need to test if these size are equals.
         int resultPosition = bsmItem.intVal;

         for (int p = 0; p < length; p++) {
            if (data[position + p] != data[resultPosition + p]) {
               bsmItem = bsmItem.next;
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
      constantPool.newUTF8("BootstrapMethods");
      //noinspection ConstantConditions
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
      // Finds the "BootstrapMethods" attribute.
      int u = cr.getAttributesStartIndex();
      char[] c = cr.buf;
      boolean found = false;

      for (int i = cr.readUnsignedShort(u); i > 0; i--) {
         String attrName = cr.readUTF8(u + 2);

         if ("BootstrapMethods".equals(attrName)) {
            found = true;
            break;
         }

         u += 6 + cr.readInt(u + 4);
      }

      if (!found) {
         return;
      }

      // Copies the bootstrap methods in the class writer.
      bootstrapMethodsCount = cr.readUnsignedShort(u + 8);

      for (int j = 0, v = u + 10; j < bootstrapMethodsCount; j++) {
         int position = v - u - 10;
         int hashCode = cr.readConst(cr.readUnsignedShort(v)).hashCode();

         for (int k = cr.readUnsignedShort(v + 2); k > 0; k--) {
            hashCode ^= cr.readConst(cr.readUnsignedShort(v + 4)).hashCode();
            v += 2;
         }

         v += 4;
         Item item = new Item(j);
         item.set(position, hashCode & 0x7FFFFFFF);
         int index = item.hashCode % items.length;
         item.next = items[index];
         items[index] = item;
      }

      int attrSize = cr.readInt(u + 4);
      bootstrapMethods = new ByteVector(attrSize + 62);
      bootstrapMethods.putByteArray(cr.code, u + 10, attrSize - 2);
   }
}

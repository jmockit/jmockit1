package mockit.external.asm;

final class BootstrapMethods
{
   private final ClassWriter cw;

   /**
    * The number of entries in the BootstrapMethods attribute.
    */
   int bootstrapMethodsCount;

   /**
    * The BootstrapMethods attribute.
    */
   ByteVector bootstrapMethods;

   BootstrapMethods(ClassWriter cw) { this.cw = cw; }

   Item add(String name, String desc, Handle bsm, Object... bsmArgs) {
      ByteVector methods = bootstrapMethods;

      if (methods == null) {
         methods = bootstrapMethods = new ByteVector();
      }

      int position = methods.length; // record current position

      int hashCode = bsm.hashCode();
      Item handleItem = cw.newHandleItem(bsm);
      methods.putShort(handleItem.index);

      int argsLength = bsmArgs.length;
      methods.putShort(argsLength);

      for (int i = 0; i < argsLength; i++) {
         Object bsmArg = bsmArgs[i];
         hashCode ^= bsmArg.hashCode();
         Item constItem = cw.newConstItem(bsmArg);
         methods.putShort(constItem.index);
      }

      byte[] data = methods.data;
      int length = (1 + 1 + argsLength) << 1; // (bsm + argCount + arguments)
      hashCode &= 0x7FFFFFFF;
      Item[] items = cw.items;
      Item result = items[hashCode % items.length];

   loop:
      while (result != null) {
         if (result.type != ConstantPoolItemType.BSM || result.hashCode != hashCode) {
            result = result.next;
            continue;
         }

         // Because the data encode the size of the argument we don't need to test if these size are equals.
         int resultPosition = result.intVal;

         for (int p = 0; p < length; p++) {
            if (data[position + p] != data[resultPosition + p]) {
               result = result.next;
               continue loop;
            }
         }

         break;
      }

      int bootstrapMethodIndex;

      if (result != null) {
         bootstrapMethodIndex = result.index;
         methods.length = position; // revert to old position
      }
      else {
         bootstrapMethodIndex = bootstrapMethodsCount++;
         result = new Item(bootstrapMethodIndex);
         result.set(position, hashCode);
         cw.put(result);
      }

      // Now, create the InvokeDynamic constant.
      Item key3 = cw.key3;
      key3.set(name, desc, bootstrapMethodIndex);
      result = cw.get(key3);

      if (result == null) {
         cw.put122(ConstantPoolItemType.INDY, bootstrapMethodIndex, cw.newNameType(name, desc));
         result = new Item(cw.index++, key3);
         cw.put(result);
      }

      return result;
   }

   boolean hasMethods() { return bootstrapMethods != null; }

   int getSize() {
      cw.newUTF8("BootstrapMethods");
      return 8 + bootstrapMethods.length;
   }

   void put(ByteVector out) {
      if (hasMethods()) {
         out.putShort(cw.newUTF8("BootstrapMethods"));
         out.putInt(bootstrapMethods.length + 2).putShort(bootstrapMethodsCount);
         out.putByteVector(bootstrapMethods);
      }
   }

   /**
    * Copies the bootstrap method data into the {@link #cw ClassWriter}.
    */
   void copyBootstrapMethods(Item[] items, char[] c) {
      ClassReader cr = cw.cr;

      // Finds the "BootstrapMethods" attribute.
      int u = cr.getAttributesStartIndex();
      boolean found = false;

      for (int i = cr.readUnsignedShort(u); i > 0; --i) {
         String attrName = cr.readUTF8(u + 2, c);

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
         int hashCode = cr.readConst(cr.readUnsignedShort(v), c).hashCode();

         for (int k = cr.readUnsignedShort(v + 2); k > 0; --k) {
            hashCode ^= cr.readConst(cr.readUnsignedShort(v + 4), c).hashCode();
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
      bootstrapMethods.putByteArray(cr.b, u + 10, attrSize - 2);
   }
}

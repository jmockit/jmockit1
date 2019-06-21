package mockit.asm.classes;

import javax.annotation.*;

import mockit.asm.constantPool.*;
import mockit.asm.util.*;
import static mockit.asm.jvmConstants.ConstantPoolTypes.*;

/**
 * Generates the "BootstrapMethods" attribute in a class file being written by a {@link ClassWriter}.
 */
final class BootstrapMethodsWriter extends AttributeWriter
{
   @Nonnull private final ByteVector bootstrapMethods;
   @Nonnegative private final int bootstrapMethodsCount;
   @Nonnegative private final int bsmStartCodeIndex;

   BootstrapMethodsWriter(@Nonnull ConstantPoolGeneration cp, @Nonnull ClassReader cr) {
      super(cp);

      int attrSize = cr.readInt();
      bootstrapMethods = new ByteVector(attrSize + 62);
      bootstrapMethodsCount = cr.readUnsignedShort();

      bsmStartCodeIndex = cr.codeIndex;
      bootstrapMethods.putByteArray(cr.code, bsmStartCodeIndex, attrSize - 2);
   }

   /**
    * Copies the bootstrap method data from the given {@link ClassReader}.
    */
   void copyBootstrapMethods(@Nonnull ClassReader cr, @Nonnull Item[] items) {
      int previousCodeIndex = cr.codeIndex;
      cr.codeIndex = bsmStartCodeIndex;

      for (int bsmIndex = 0, bsmCount = bootstrapMethodsCount; bsmIndex < bsmCount; bsmIndex++) {
         copyBootstrapMethod(cr, items, bsmIndex);
      }

      cr.codeIndex = previousCodeIndex;
   }

   private void copyBootstrapMethod(@Nonnull ClassReader cr, @Nonnull Item[] items, @Nonnegative int bsmIndex) {
      int position = cr.codeIndex - bsmStartCodeIndex;
      MethodHandle bsm = cr.readMethodHandle();
      int hashCode = bsm.hashCode();

      for (int bsmArgCount = cr.readUnsignedShort(); bsmArgCount > 0; bsmArgCount--) {
         Object bsmArg = cr.readConstItem();
         hashCode ^= bsmArg.hashCode();
      }

      BootstrapMethodItem item = new BootstrapMethodItem(bsmIndex, position, hashCode);
      item.setNext(items);
   }

   /**
    * Adds an invokedynamic reference to the constant pool of the class being built.
    * Does nothing if the constant pool already contains a similar item.
    *
    * @param name    name of the invoked method
    * @param desc    descriptor of the invoke method
    * @param bsm     the bootstrap method
    * @param bsmArgs the bootstrap method constant arguments
    * @return a new or an already existing invokedynamic type reference item
    */
   @Nonnull
   DynamicItem addInvokeDynamicReference(
      @Nonnull String name, @Nonnull String desc, @Nonnull MethodHandle bsm, @Nonnull Object... bsmArgs
   ) {
      ByteVector methods = bootstrapMethods;
      int position = methods.getLength(); // record current position

      MethodHandleItem methodHandleItem = cp.newMethodHandleItem(bsm);
      methods.putShort(methodHandleItem.index);

      int argsLength = bsmArgs.length;
      methods.putShort(argsLength);

      int hashCode = bsm.hashCode();
      hashCode = putBSMArgs(hashCode, bsmArgs);
      hashCode &= 0x7FFFFFFF;

      methods.setLength(position); // revert to old position

      BootstrapMethodItem bsmItem = getBSMItem(hashCode);
      DynamicItem result = cp.createDynamicItem(INDY, name, desc, bsmItem.index);
      return result;
   }

   private int putBSMArgs(int hashCode, @Nonnull Object[] bsmArgs) {
      for (Object bsmArg : bsmArgs) {
         hashCode ^= bsmArg.hashCode();

         Item constItem = cp.newConstItem(bsmArg);
         bootstrapMethods.putShort(constItem.index);
      }

      return hashCode;
   }

   @Nonnull
   private BootstrapMethodItem getBSMItem(@Nonnegative int hashCode) {
      Item item = cp.getItem(hashCode);

      while (item != null) {
         if ((item instanceof BootstrapMethodItem) && item.getHashCode() == hashCode) {
            return (BootstrapMethodItem) item;
         }

         item = item.getNext();
      }

      throw new IllegalStateException("BootstrapMethodItem not found for hash code " + hashCode);
   }

   @Nonnegative @Override
   public int getSize() { return 8 + bootstrapMethods.getLength(); }

   @Override
   public void put(@Nonnull ByteVector out) {
      setAttribute("BootstrapMethods");
      put(out, 2 + bootstrapMethods.getLength());
      out.putShort(bootstrapMethodsCount);
      out.putByteVector(bootstrapMethods);
   }
}
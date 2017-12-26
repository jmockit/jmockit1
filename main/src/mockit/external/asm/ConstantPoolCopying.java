package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.Item.Type.*;

/**
 * Copies the constant pool data from a {@link ClassReader} into a {@link ClassWriter}.
 */
final class ConstantPoolCopying
{
   @Nonnull private final ClassReader source;

   ConstantPoolCopying(@Nonnull ClassReader source) { this.source = source; }

   @SuppressWarnings({"OverlyComplexMethod", "OverlyLongMethod"})
   void copyPool(@Nonnull ClassWriter destination) {
      byte[] code = source.code;
      int[] items = source.items;
      int itemCount = items.length;
      Item[] items2 = new Item[itemCount];

      for (int itemIndex = 1; itemIndex < itemCount; itemIndex++) {
         int itemCodeIndex = items[itemIndex];
         int itemType = code[itemCodeIndex - 1];
         Item item;

         switch (itemType) {
            case FIELD: case METH: case IMETH:
               item = copyFieldOrMethodReferenceItem(itemType, itemCodeIndex, itemIndex);
               break;
            case INT:
               item = copyIntItem(itemCodeIndex, itemIndex);
               break;
            case FLOAT:
               item = copyFloatItem(itemCodeIndex, itemIndex);
               break;
            case NAME_TYPE:
               item = copyNameAndTypeItem(itemCodeIndex, itemIndex);
               break;
            case LONG:
               item = copyLongItem(itemCodeIndex, itemIndex);
               itemIndex++;
               break;
            case DOUBLE:
               item = copyDoubleItem(itemCodeIndex, itemIndex);
               itemIndex++;
               break;
            case UTF8:
               item = copyUTF8Item(itemIndex);
               break;
            case HANDLE:
               item = copyHandleItem(itemCodeIndex, itemIndex);
               break;
            case INDY:
               item = copyInvokeDynamicItem(destination.bootstrapMethods, items2, itemCodeIndex, itemIndex);
               break;
            // case STR|CLASS|MTYPE:
            default:
               item = copyNameReferenceItem(itemType, itemCodeIndex, itemIndex);
         }

         item.setNext(items2);
      }

      int off = items[1] - 1;
      destination.cp.copy(code, off, source.header, items2);
   }

   @Nonnull
   private Item copyIntItem(@Nonnegative int codeIndex, @Nonnegative int itemIndex) {
      int itemValue = source.readInt(codeIndex);
      IntItem item = new IntItem(itemIndex);
      item.set(itemValue);
      return item;
   }

   @Nonnull
   private Item copyLongItem(@Nonnegative int codeIndex, @Nonnegative int itemIndex) {
      long itemValue = source.readLong(codeIndex);
      LongItem item = new LongItem(itemIndex);
      item.set(itemValue);
      return item;
   }

   @Nonnull
   private Item copyFloatItem(@Nonnegative int codeIndex, @Nonnegative int itemIndex) {
      float itemValue = source.readFloat(codeIndex);
      FloatItem item = new FloatItem(itemIndex);
      item.set(itemValue);
      return item;
   }

   @Nonnull
   private Item copyDoubleItem(@Nonnegative int codeIndex, @Nonnegative int itemIndex) {
      double itemValue = source.readDouble(codeIndex);
      DoubleItem item = new DoubleItem(itemIndex);
      item.set(itemValue);
      return item;
   }

   @Nonnull
   private Item copyUTF8Item(@Nonnegative int itemIndex) {
      String string = source.readString(itemIndex);
      return new StringItem(itemIndex, UTF8, string);
   }

   @Nonnull
   private Item copyNameReferenceItem(int type, @Nonnegative int codeIndex, @Nonnegative int itemIndex) {
      String string = source.readNonnullUTF8(codeIndex);
      return new StringItem(itemIndex, type, string);
   }

   @Nonnull
   private Item copyNameAndTypeItem(@Nonnegative int codeIndex, @Nonnegative int itemIndex) {
      String name = source.readNonnullUTF8(codeIndex);
      String type = source.readNonnullUTF8(codeIndex + 2);

      NameAndTypeItem item = new NameAndTypeItem(itemIndex);
      item.set(name, type);
      return item;
   }

   @Nonnull
   private Item copyFieldOrMethodReferenceItem(int type, @Nonnegative int codeIndex, @Nonnegative int itemIndex) {
      int nameType = source.readItem(codeIndex + 2);
      String classDesc = source.readNonnullClass(codeIndex);
      String methodName = source.readNonnullUTF8(nameType);
      String methodDesc = source.readNonnullUTF8(nameType + 2);

      ClassMemberItem item = new ClassMemberItem(itemIndex);
      item.set(type, classDesc, methodName, methodDesc);
      return item;
   }

   @Nonnull
   private Item copyHandleItem(@Nonnegative int codeIndex, @Nonnegative int itemIndex) {
      int fieldOrMethodRef = source.readItem(codeIndex + 1);
      int nameType = source.readItem(fieldOrMethodRef + 2);

      int tag = source.readUnsignedByte(codeIndex);
      String classDesc = source.readNonnullClass(fieldOrMethodRef);
      String name = source.readNonnullUTF8(nameType);
      String desc = source.readNonnullUTF8(nameType + 2);

      Handle handle = new Handle(tag, classDesc, name, desc);
      HandleItem item = new HandleItem(itemIndex);
      item.set(handle);
      return item;
   }

   @Nonnull
   private Item copyInvokeDynamicItem(
      @Nonnull BootstrapMethods bootstrapMethods, @Nonnull Item[] items2, @Nonnegative int codeIndex,
      @Nonnegative int itemIndex
   ) {
      bootstrapMethods.copyBootstrapMethods(source, items2);

      int nameType = source.readItem(codeIndex + 2);
      String name = source.readNonnullUTF8(nameType);
      String desc = source.readNonnullUTF8(nameType + 2);
      int bsmIndex = source.readUnsignedShort(codeIndex);

      InvokeDynamicItem item = new InvokeDynamicItem(itemIndex);
      item.set(name, desc, bsmIndex);
      return item;
   }
}

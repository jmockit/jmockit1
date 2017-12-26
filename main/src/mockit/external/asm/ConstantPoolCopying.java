package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.Item.Type.*;

/**
 * Copies the constant pool data from a {@link ClassReader} into a {@link ClassWriter}.
 */
final class ConstantPoolCopying
{
   @Nonnull private final ClassReader source;
   @Nonnull private final ClassWriter destination;
   @Nonnegative private int itemIndex;

   ConstantPoolCopying(@Nonnull ClassReader source, @Nonnull ClassWriter destination) {
      this.source = source;
      this.destination = destination;
   }

   void copyPool() {
      int[] items = source.items;
      int itemCount = items.length;
      Item[] newItems = new Item[itemCount];

      for (itemIndex = 1; itemIndex < itemCount; itemIndex++) {
         source.codeIndex = items[itemIndex] - 1;
         int itemType = source.readSignedByte();

         Item newItem = copyItem(itemType, newItems);
         newItem.setNext(newItems);
      }

      int off = items[1] - 1;
      destination.cp.copy(source.code, off, source.header, newItems);
   }

   @Nonnull @SuppressWarnings("OverlyComplexMethod")
   private Item copyItem(int itemType, @Nonnull Item[] newItems) {
      switch (itemType) {
         case FIELD: case METH: case IMETH: return copyFieldOrMethodReferenceItem(itemType);
         case INT:       return copyIntItem();
         case FLOAT:     return copyFloatItem();
         case NAME_TYPE: return copyNameAndTypeItem();
         case LONG:      return copyLongItem();
         case DOUBLE:    return copyDoubleItem();
         case UTF8:      return copyUTF8Item();
         case HANDLE:    return copyHandleItem();
         case INDY:      return copyInvokeDynamicItem(newItems);
      // case STR|CLASS|MTYPE:
         default: return copyNameReferenceItem(itemType);
      }
   }

   @Nonnull
   private Item copyIntItem() {
      int itemValue = source.readInt();
      IntItem item = new IntItem(itemIndex);
      item.setValue(itemValue);
      return item;
   }

   @Nonnull
   private Item copyLongItem() {
      long itemValue = source.readLong();
      LongItem item = new LongItem(itemIndex);
      item.setValue(itemValue);
      itemIndex++;
      return item;
   }

   @Nonnull
   private Item copyFloatItem() {
      float itemValue = source.readFloat();
      FloatItem item = new FloatItem(itemIndex);
      item.set(itemValue);
      return item;
   }

   @Nonnull
   private Item copyDoubleItem() {
      double itemValue = source.readDouble();
      DoubleItem item = new DoubleItem(itemIndex);
      item.set(itemValue);
      itemIndex++;
      return item;
   }

   @Nonnull
   private Item copyUTF8Item() {
      String string = source.readString(itemIndex);
      return new StringItem(itemIndex, UTF8, string);
   }

   @Nonnull
   private Item copyNameReferenceItem(int type) {
      String string = source.readNonnullUTF8();
      return new StringItem(itemIndex, type, string);
   }

   @Nonnull
   private Item copyNameAndTypeItem() {
      String name = source.readNonnullUTF8();
      String type = source.readNonnullUTF8();

      NameAndTypeItem item = new NameAndTypeItem(itemIndex);
      item.set(name, type);
      return item;
   }

   @Nonnull
   private Item copyFieldOrMethodReferenceItem(int type) {
      String classDesc = source.readNonnullClass();
      int nameCodeIndex = source.readItem();
      String methodName = source.readNonnullUTF8(nameCodeIndex);
      String methodDesc = source.readNonnullUTF8(nameCodeIndex + 2);

      ClassMemberItem item = new ClassMemberItem(itemIndex);
      item.set(type, classDesc, methodName, methodDesc);
      return item;
   }

   @Nonnull
   private Item copyHandleItem() {
      int tag = source.readUnsignedByte();

      int fieldOrMethodRef = source.readItem();
      int nameCodeIndex = source.readItem(fieldOrMethodRef + 2);

      String classDesc = source.readNonnullClass(fieldOrMethodRef);
      String name = source.readNonnullUTF8(nameCodeIndex);
      String desc = source.readNonnullUTF8(nameCodeIndex + 2);

      Handle handle = new Handle(tag, classDesc, name, desc);
      HandleItem item = new HandleItem(itemIndex);
      item.set(handle);
      return item;
   }

   @Nonnull
   private Item copyInvokeDynamicItem(@Nonnull Item[] newItems) {
      destination.bootstrapMethods.copyBootstrapMethods(source, newItems);

      int bsmIndex = source.readUnsignedShort();
      int nameCodeIndex = source.readItem();
      String name = source.readNonnullUTF8(nameCodeIndex);
      String desc = source.readNonnullUTF8(nameCodeIndex + 2);

      InvokeDynamicItem item = new InvokeDynamicItem(itemIndex);
      item.set(name, desc, bsmIndex);
      return item;
   }
}

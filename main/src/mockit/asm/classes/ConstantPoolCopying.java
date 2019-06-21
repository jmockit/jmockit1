package mockit.asm.classes;

import javax.annotation.*;

import mockit.asm.constantPool.*;
import mockit.asm.util.*;
import static mockit.asm.jvmConstants.ConstantPoolTypes.*;

/**
 * Copies the constant pool data from a {@link ClassReader} into a {@link ClassWriter}.
 */
final class ConstantPoolCopying
{
   @Nonnull private final ClassReader source;
   @Nonnull private final ClassWriter destination;
   @Nonnull private final Item[] newItems;
   @Nonnegative private int itemIndex;

   ConstantPoolCopying(@Nonnull ClassReader source, @Nonnull ClassWriter destination) {
      this.source = source;
      this.destination = destination;
      newItems = new Item[source.items.length];
   }

   void copyPool(@Nullable BootstrapMethodsWriter bootstrapMethods) {
      if (bootstrapMethods != null) {
         bootstrapMethods.copyBootstrapMethods(source, newItems);
      }

      int[] items = source.items;
      int itemCount = items.length;

      for (itemIndex = 1; itemIndex < itemCount; itemIndex++) {
         source.codeIndex = items[itemIndex] - 1;
         int itemType = source.readSignedByte();

         Item newItem = copyItem(itemType);
         newItem.setNext(newItems);
      }

      int off = items[1] - 1;
      destination.getConstantPoolGeneration().copy(source.code, off, source.header, newItems);
   }

   @Nonnull @SuppressWarnings("OverlyComplexMethod")
   private Item copyItem(int itemType) {
      switch (itemType) {
         case UTF8:      return copyUTF8Item();
         case INT:       return copyIntItem();
         case FLOAT:     return copyFloatItem();
         case LONG:      return copyLongItem();
         case DOUBLE:    return copyDoubleItem();
         case FIELD:
         case METH:
         case IMETH:     return copyFieldOrMethodReferenceItem(itemType);
         case NAME_TYPE: return copyNameAndTypeItem();
         case HANDLE:    return copyHandleItem();
         case CONDY:
         case INDY:      return copyDynamicItem(itemType);
      // case STR|CLASS|MTYPE:
         default:        return copyNameReferenceItem(itemType);
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
      String strVal = source.readString(itemIndex);
      return new StringItem(itemIndex, UTF8, strVal);
   }

   @Nonnull
   private Item copyNameReferenceItem(int type) {
      String strVal = source.readNonnullUTF8();
      return new StringItem(itemIndex, type, strVal);
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

      MethodHandle handle = new MethodHandle(tag, classDesc, name, desc);
      MethodHandleItem item = new MethodHandleItem(itemIndex);
      item.set(handle);
      return item;
   }

   @Nonnull
   private Item copyDynamicItem(int type) {
      int bsmIndex = source.readUnsignedShort();
      int nameCodeIndex = source.readItem();
      String name = source.readNonnullUTF8(nameCodeIndex);
      String desc = source.readNonnullUTF8(nameCodeIndex + 2);

      DynamicItem item = new DynamicItem(itemIndex);
      item.set(type, name, desc, bsmIndex);
      return item;
   }
}
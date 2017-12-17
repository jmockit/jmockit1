package mockit.external.asm;

import javax.annotation.*;

import mockit.external.asm.Handle.*;
import mockit.internal.util.*;
import static mockit.external.asm.ConstantPoolGeneration.ItemType.*;
import static mockit.internal.util.ClassLoad.OBJECT;

/**
 * Allows the constant pool for a classfile to be created from scratch, when that classfile itself is being generated or
 * modified from an existing class file.
 */
final class ConstantPoolGeneration
{
   /**
    * Constants for types of items in the constant pool of a class.
    */
   interface ItemType
   {
      int CLASS     =  7; // CONSTANT_Class
      int FIELD     =  9; // CONSTANT_Fieldref
      int METH      = 10; // CONSTANT_Methodref
      int IMETH     = 11; // CONSTANT_InterfaceMethodref
      int STR       =  8; // CONSTANT_String
      int INT       =  3; // CONSTANT_Integer
      int FLOAT     =  4; // CONSTANT_Float
      int LONG      =  5; // CONSTANT_Long
      int DOUBLE    =  6; // CONSTANT_Double
      int NAME_TYPE = 12; // CONSTANT_NameAndType
      int UTF8      =  1; // CONSTANT_Utf8
      int MTYPE     = 16; // CONSTANT_MethodType
      int HANDLE    = 15; // CONSTANT_MethodHandle
      int INDY      = 18; // CONSTANT_InvokeDynamic

      /**
       * The base value for all CONSTANT_MethodHandle constant pool items.
       * Internally, ASM stores the 9 variations of CONSTANT_MethodHandle into 9 different items.
       */
      int HANDLE_BASE = 20;

      /**
       * The type of BootstrapMethod items. These items are stored in a special class attribute named "BootstrapMethods"
       * and not in the constant pool.
       */
      int BSM = 33;
   }

   /**
    * The constant pool of the class file being generated/modified.
    */
   @Nonnull private final ByteVector pool;

   /**
    * The constant pool's hash table data.
    */
   @Nonnull private Item[] items;

   /**
    * The threshold of the constant pool's hash table.
    */
   @Nonnegative private int threshold;

   /**
    * Index of the next item to be added in the constant pool.
    */
   @Nonnegative private int index;

   /**
    * A reusable key used to look for items in the {@link #items} hash table.
    */
   @Nonnull private final StringItem reusableUTF8Item;

   /**
    * A reusable key used to look for items in the {@link #items} hash table.
    */
   @Nonnull private final StringItem reusableStringItem;

   /**
    * A reusable key used to look for items in the {@link #items} hash table.
    */
   @Nonnull private final NameAndTypeItem reusableNameTypeItem;

   /**
    * A reusable key used to look for items in the {@link #items} hash table.
    */
   @Nonnull private final ClassMemberItem reusableClassMemberItem;

   /**
    * A reusable key used to look for items in the {@link #items} hash table.
    */
   @Nonnull private final IntItem reusableIntItem;

   /**
    * A reusable key used to look for items in the {@link #items} hash table.
    */
   @Nonnull private final LongItem reusableLongItem;

   /**
    * A reusable key used to look for items in the {@link #items} hash table.
    */
   @Nonnull private final FloatItem reusableFloatItem;

   /**
    * A reusable key used to look for items in the {@link #items} hash table.
    */
   @Nonnull private final DoubleItem reusableDoubleItem;

   /**
    * A reusable key used to look for items in the {@link #items} hash table.
    */
   @Nonnull private final HandleItem reusableHandleItem;

   /**
    * A reusable key used to look for items in the {@link #items} hash table.
    */
   @Nonnull private final InvokeDynamicItem reusableInvokeDynamicItem;

   /**
    * A type table used to temporarily store internal names that will not necessarily be stored in the constant pool.
    * This type table is used by the control flow and data flow analysis algorithm to compute stack map frames from
    * scratch. This array associates to each index <tt>i</tt> the <tt>TypeTableItem</tt> whose index is <tt>i</tt>. All
    * <tt>TypeTableItem</tt> objects stored in this array are also stored in the {@link #items} hash table. These two
    * arrays allow to retrieve an <tt>Item</tt> from its index or, conversely, to get the index of an <tt>Item</tt> from
    * its value. Each <tt>TypeTableItem</tt> stores an internal name in its {@link TypeTableItem#typeDesc} field.
    */
   private TypeTableItem[] typeTable;

   /**
    * Number of elements in the {@link #typeTable} array.
    */
   private short typeCount;

   @Nonnull private final NormalTypeTableItem reusableNormalItem;
   @Nonnull private final UninitializedTypeTableItem reusableUninitializedItem;
   @Nonnull private final MergedTypeTableItem reusableMergedItem;

   ConstantPoolGeneration() {
      pool = new ByteVector();
      items = new Item[256];
      threshold = (int) (0.75d * items.length);
      index = 1;
      reusableUTF8Item = new StringItem(0);
      reusableStringItem = new StringItem(0);
      reusableNameTypeItem = new NameAndTypeItem(0);
      reusableClassMemberItem = new ClassMemberItem(0);
      reusableIntItem = new IntItem(0);
      reusableLongItem = new LongItem(0);
      reusableFloatItem = new FloatItem(0);
      reusableDoubleItem = new DoubleItem(0);
      reusableHandleItem = new HandleItem(0);
      reusableInvokeDynamicItem = new InvokeDynamicItem(0);
      reusableNormalItem = new NormalTypeTableItem();
      reusableUninitializedItem = new UninitializedTypeTableItem();
      reusableMergedItem = new MergedTypeTableItem();
   }

   /**
    * Adds an UTF8 string to the constant pool of the class being built.
    * Does nothing if the constant pool already contains a similar item.
    *
    * @param value the String value.
    * @return the index of a new or already existing UTF8 item.
    */
   @Nonnegative
   int newUTF8(@Nonnull String value) {
      reusableUTF8Item.set(UTF8, value);

      StringItem result = get(reusableUTF8Item);

      if (result == null) {
         pool.putByte(UTF8).putUTF8(value);

         result = new StringItem(index++, reusableUTF8Item);
         put(result);
      }

      return result.index;
   }

   /**
    * Adds a class reference to the constant pool of the class being built.
    * Does nothing if the constant pool already contains a similar item.
    *
    * @param internalName the internal name of the class.
    * @return the index of a new or already existing class reference item.
    */
   @Nonnegative
   int newClass(@Nonnull String internalName) {
      return newClassItem(internalName).index;
   }

   /**
    * Adds a class reference to the constant pool of the class being built.
    * Does nothing if the constant pool already contains a similar item.
    *
    * @param internalName the internal name of the class.
    * @return a new or already existing class reference item.
    */
   @Nonnull
   StringItem newClassItem(@Nonnull String internalName) {
      return newItem(CLASS, internalName);
   }

   /**
    * Adds a method type reference to the constant pool of the class being built.
    * Does nothing if the constant pool already contains a similar item.
    *
    * @param methodDesc method descriptor of the method type.
    * @return a new or already existing method type reference item.
    */
   @Nonnull
   private StringItem newMethodTypeItem(@Nonnull String methodDesc) {
      return newItem(MTYPE, methodDesc);
   }

   /**
    * Adds a string to the constant pool of the class being built.
    * Does nothing if the constant pool already contains a similar item.
    *
    * @param value the String value.
    * @return a new or already existing string item.
    */
   @Nonnull
   private StringItem newString(@Nonnull String value) {
      return newItem(STR, value);
   }

   @Nonnull
   private StringItem newItem(int type, @Nonnull String value) {
      reusableStringItem.set(type, value);

      StringItem result = get(reusableStringItem);

      if (result == null) {
         int itemIndex = newUTF8(value);
         pool.put12(type, itemIndex);

         result = new StringItem(index++, reusableStringItem);
         put(result);
      }

      return result;
   }

   /**
    * Adds a handle to the constant pool of the class being built.
    * Does nothing if the constant pool already contains a similar item.
    *
    * @return a new or an already existing method type reference item.
    */
   @Nonnull
   HandleItem newHandleItem(@Nonnull Handle handle) {
      int tag = handle.tag;
      reusableHandleItem.set(handle);

      HandleItem result = get(reusableHandleItem);

      if (result == null) {
         ClassMemberItem item;

         if (tag <= Tag.PUTSTATIC) {
            item = newFieldItem(handle.owner, handle.name, handle.desc);
         }
         else {
            boolean itf = tag == Tag.INVOKEINTERFACE;
            item = newMethodItem(handle.owner, handle.name, handle.desc, itf);
         }

         pool.put11(HANDLE, tag).putShort(item.index);

         result = new HandleItem(index++, reusableHandleItem);
         put(result);
      }

      return result;
   }

   /**
    * Adds a field reference to the constant pool of the class being built.
    * Does nothing if the constant pool already contains a similar item.
    *
    * @param owner the internal name of the field's owner class.
    * @param name  the field's name.
    * @param desc  the field's descriptor.
    * @return a new or already existing field reference item.
    */
   @Nonnull
   ClassMemberItem newFieldItem(@Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
      return newClassMemberItem(FIELD, owner, name, desc);
   }

   /**
    * Adds a method reference to the constant pool of the class being built.
    * Does nothing if the constant pool already contains a similar item.
    *
    * @param owner the internal name of the method's owner class.
    * @param name  the method's name.
    * @param desc  the method's descriptor.
    * @param itf   <tt>true</tt> if <tt>owner</tt> is an interface.
    * @return a new or already existing method reference item.
    */
   @Nonnull
   ClassMemberItem newMethodItem(@Nonnull String owner, @Nonnull String name, @Nonnull String desc, boolean itf) {
      return newClassMemberItem(itf ? IMETH : METH, owner, name, desc);
   }

   @Nonnull
   private ClassMemberItem newClassMemberItem(
      int type, @Nonnull String owner, @Nonnull String name, @Nonnull String desc
   ) {
      reusableClassMemberItem.set(type, owner, name, desc);

      ClassMemberItem result = get(reusableClassMemberItem);

      if (result == null) {
         int ownerItemIndex = newClass(owner);
         int nameAndTypeItemIndex = newNameType(name, desc);
         put122(type, ownerItemIndex, nameAndTypeItemIndex);

         result = new ClassMemberItem(index++, reusableClassMemberItem);
         put(result);
      }

      return result;
   }

   /**
    * Adds an integer to the constant pool of the class being built.
    * Does nothing if the constant pool already contains a similar item.
    *
    * @param value the int value.
    * @return a new or already existing int item.
    */
   @Nonnull
   IntItem newInteger(int value) {
      reusableIntItem.set(value);

      IntItem result = get(reusableIntItem);

      if (result == null) {
         pool.putByte(INT).putInt(value);

         result = new IntItem(index++, reusableIntItem);
         put(result);
      }

      return result;
   }

   /**
    * Adds a float to the constant pool of the class being built.
    * Does nothing if the constant pool already contains a similar item.
    *
    * @param value the float value.
    * @return a new or already existing float item.
    */
   @Nonnull
   FloatItem newFloat(float value) {
      reusableFloatItem.set(value);

      FloatItem result = get(reusableFloatItem);

      if (result == null) {
         pool.putByte(FLOAT).putInt(reusableFloatItem.intVal);

         result = new FloatItem(index++, reusableFloatItem);
         put(result);
      }

      return result;
   }

   /**
    * Adds a long to the constant pool of the class being built.
    * Does nothing if the constant pool already contains a similar item.
    *
    * @param value the long value.
    * @return a new or already existing long item.
    */
   @Nonnull
   LongItem newLong(long value) {
      reusableLongItem.set(value);

      LongItem result = get(reusableLongItem);

      if (result == null) {
         pool.putByte(LONG).putLong(value);

         result = new LongItem(index, reusableLongItem);
         index += 2;
         put(result);
      }

      return result;
   }

   /**
    * Adds a double to the constant pool of the class being built.
    * Does nothing if the constant pool already contains a similar item.
    *
    * @param value the double value.
    * @return a new or already existing double item.
    */
   @Nonnull
   DoubleItem newDouble(double value) {
      reusableDoubleItem.set(value);

      DoubleItem result = get(reusableDoubleItem);

      if (result == null) {
         pool.putByte(DOUBLE).putLong(reusableDoubleItem.longVal);

         result = new DoubleItem(index, reusableDoubleItem);
         index += 2;
         put(result);
      }

      return result;
   }

   /**
    * Adds a name and type to the constant pool of the class being built.
    * Does nothing if the constant pool already contains a similar item.
    *
    * @param name a name.
    * @param desc a type descriptor.
    * @return the index of a new or already existing name and type item.
    */
   int newNameType(@Nonnull String name, @Nonnull String desc) {
      reusableNameTypeItem.set(name, desc);

      NameAndTypeItem result = get(reusableNameTypeItem);

      if (result == null) {
         int nameItemIndex = newUTF8(name);
         int descItemIndex = newUTF8(desc);
         put122(NAME_TYPE, nameItemIndex, descItemIndex);

         result = new NameAndTypeItem(index++, reusableNameTypeItem);
         put(result);
      }

      return result.index;
   }

   /**
    * Adds a number or string constant to the constant pool of the class being built.
    * Does nothing if the constant pool already contains a similar item.
    *
    * @param cst the value of the constant to be added to the constant pool. This parameter must be an {@link Integer},
    *            a {@link Float}, a {@link Long}, a {@link Double}, a {@link String} or a {@link JavaType}.
    * @return a new or already existing constant item with the given value.
    */
   @Nonnull
   Item newConstItem(@Nonnull Object cst) {
      if (cst instanceof String) {
         return newString((String) cst);
      }

      if (cst instanceof Number) {
         return newNumberItem((Number) cst);
      }

      if (cst instanceof Character) {
         return newInteger((int) (Character) cst);
      }

      if (cst instanceof Boolean) {
         int val = (Boolean) cst ? 1 : 0;
         return newInteger(val);
      }

      if (cst instanceof ReferenceType) {
         String typeDesc = ((ReferenceType) cst).getInternalName();
         return cst instanceof MethodType ? newMethodTypeItem(typeDesc) : newClassItem(typeDesc);
      }

      if (cst instanceof PrimitiveType) {
         String typeDesc = ((PrimitiveType) cst).getDescriptor();
         return newClassItem(typeDesc);
      }

      if (cst instanceof Handle) {
         return newHandleItem((Handle) cst);
      }

      throw new IllegalArgumentException("value " + cst);
   }

   @Nonnull
   private Item newNumberItem(Number cst) {
      if (cst instanceof Float) {
         return newFloat(cst.floatValue());
      }

      if (cst instanceof Long) {
         return newLong(cst.longValue());
      }

      if (cst instanceof Double) {
         return newDouble(cst.doubleValue());
      }

      return newInteger(cst.intValue());
   }

   /**
    * Adds the given internal name to {@link #typeTable} and returns its index.
    * Does nothing if the type table already contains this internal name.
    *
    * @param type the internal name to be added to the type table.
    * @return the index of this internal name in the type table.
    */
   int addNormalType(@Nonnull String type) {
      reusableNormalItem.set(type);

      TypeTableItem result = get(reusableNormalItem);

      if (result == null) {
         result = new NormalTypeTableItem(++typeCount, reusableNormalItem);
         addToTypeTable(result);
      }

      return result.index;
   }

   /**
    * Adds the given "uninitialized" type to {@link #typeTable} and returns its index.
    * This method is used for UNINITIALIZED types, made of an internal name and a bytecode offset.
    *
    * @param type   the internal name to be added to the type table.
    * @param offset the bytecode offset of the NEW instruction that created this UNINITIALIZED type value.
    * @return the index of this internal name in the type table.
    */
   int addUninitializedType(@Nonnull String type, @Nonnegative int offset) {
      reusableUninitializedItem.set(type, offset);

      TypeTableItem result = get(reusableUninitializedItem);

      if (result == null) {
         result = new UninitializedTypeTableItem(++typeCount, reusableUninitializedItem);
         addToTypeTable(result);
      }

      return result.index;
   }

   private void addToTypeTable(@Nonnull TypeTableItem newItem) {
      put(newItem);

      if (typeTable == null) {
         typeTable = new TypeTableItem[16];
      }

      int newItemIndex = typeCount;
      enlargeTypeTableIfNeeded(newItemIndex);
      typeTable[newItemIndex] = newItem;
   }

   private void enlargeTypeTableIfNeeded(@Nonnegative int newItemIndex) {
      int currentTypeCount = typeTable.length;

      if (newItemIndex == currentTypeCount) {
         TypeTableItem[] newTable = new TypeTableItem[2 * currentTypeCount];
         System.arraycopy(typeTable, 0, newTable, 0, currentTypeCount);
         typeTable = newTable;
      }
   }

   /**
    * Returns the index of the common super type of the two given types. This method calls {@link #getCommonSuperClass}
    * and caches the result in the {@link #items} hash table to speedup future calls with the same parameters.
    *
    * @param type1 index of an internal name in {@link #typeTable}.
    * @param type2 index of an internal name in {@link #typeTable}.
    * @return the index of the common super type of the two given types.
    */
   int getMergedType(@Nonnegative int type1, @Nonnegative int type2) {
      reusableMergedItem.set(type1, type2);

      MergedTypeTableItem result = get(reusableMergedItem);

      if (result == null) {
         String type1Desc = getInternalName(type1);
         String type2Desc = getInternalName(type2);
         String commonSuperClass = getCommonSuperClass(type1Desc, type2Desc);
         reusableMergedItem.commonSuperTypeIndex = addNormalType(commonSuperClass);

         result = new MergedTypeTableItem(reusableMergedItem);
         put(result);
      }

      return result.commonSuperTypeIndex;
   }

   /**
    * Returns the common super type of the two given types. The default implementation of this method <i>loads</i> the
    * two given classes and uses the java.lang.Class methods to find the common super class. It can be overridden to
    * compute this common super type in other ways, in particular without actually loading any class, or to take into
    * account the class that is currently being generated by this ClassWriter, which can of course not be loaded since
    * it is under construction.
    *
    * @param type1 the internal name of a class.
    * @param type2 the internal name of another class.
    * @return the internal name of the common super class of the two given classes.
    */
   @Nonnull
   private static String getCommonSuperClass(@Nonnull String type1, @Nonnull String type2) {
      // Reimplemented to avoid "duplicate class definition" errors.
      String class1 = type1;
      String class2 = type2;

      while (true) {
         if (OBJECT.equals(class1) || OBJECT.equals(class2)) {
            return OBJECT;
         }

         String superClass = ClassLoad.whichIsSuperClass(class1, class2);

         if (superClass != null) {
            return superClass;
         }

         class1 = ClassLoad.getSuperClass(class1);
         class2 = ClassLoad.getSuperClass(class2);

         if (class1.equals(class2)) {
            return class1;
         }
      }
   }

   @Nonnull
   String getInternalName(@Nonnegative int typeTableIndex) {
      TypeTableItem typeTableItem = typeTable[typeTableIndex]; // Normal or Uninitialized
      return typeTableItem.typeDesc;
   }

   @Nonnull
   UninitializedTypeTableItem getUninitializedItemValue(@Nonnegative int typeTableIndex) {
      return (UninitializedTypeTableItem) typeTable[typeTableIndex];
   }

   @Nullable
   Item getItem(int itemHashCode) { return items[itemHashCode % items.length]; }

   /**
    * Returns the constant pool's hash table item which is equal to the given item.
    *
    * @param key a constant pool item.
    * @return the constant pool's hash table item which is equal to the given item, or <tt>null</tt> if there is no
    * such item.
    */
   @Nullable
   private <I extends Item> I get(@Nonnull I key) {
      Item item = getItem(key.hashCode);
      int keyType = key.type;

      while (item != null && (item.type != keyType || !key.isEqualTo(item))) {
         item = item.next;
      }

      //noinspection unchecked
      return (I) item;
   }

   /**
    * Puts the given item in the constant pool's hash table. The hash table <i>must</i> not already contains this item.
    *
    * @param item the item to be added to the constant pool's hash table.
    */
   void put(@Nonnull Item item) {
      resizeItemArrayIfNeeded();
      item.setNext(items);
   }

   private void resizeItemArrayIfNeeded() {
      if (index + typeCount > threshold) {
         int ll = items.length;
         int nl = ll * 2 + 1;
         Item[] newItems = new Item[nl];

         for (int l = ll - 1; l >= 0; l--) {
            Item j = items[l];
            put(newItems, j);
         }

         items = newItems;
         threshold = (int) (nl * 0.75);
      }
   }

   private static void put(@Nonnull Item[] newItems, @Nullable Item item) {
      while (item != null) {
         Item k = item.next;
         item.setNext(newItems);
         item = k;
      }
   }

   /**
    * Puts one byte and two shorts into the constant pool.
    *
    * @param b  a byte.
    * @param s1 a short.
    * @param s2 another short.
    */
   private void put122(int b, int s1, int s2) {
      pool.put12(b, s1).putShort(s2);
   }

   @Nonnegative
   int getSize() { return pool.length; }

   void checkConstantPoolMaxSize() {
      if (index > 0xFFFF) {
         throw new RuntimeException("Class file too large!");
      }
   }

   void put(@Nonnull ByteVector out) {
      out.putShort(index).putByteVector(pool);
   }

   void copy(@Nullable byte[] b, @Nonnegative int off, @Nonnegative int header, @Nonnull Item[] items) {
      pool.putByteArray(b, off, header - off);
      this.items = items;

      int ll = items.length;
      threshold = (int) (0.75d * ll);
      index = ll;
   }

   @Nonnull
   InvokeDynamicItem createInvokeDynamicItem(@Nonnull String name, @Nonnull String desc, @Nonnegative int bsmIndex) {
      reusableInvokeDynamicItem.set(name, desc, bsmIndex);

      InvokeDynamicItem result = get(reusableInvokeDynamicItem);

      if (result == null) {
         int nameAndTypeItemIndex = newNameType(name, desc);
         put122(INDY, bsmIndex, nameAndTypeItemIndex);

         result = new InvokeDynamicItem(index++, reusableInvokeDynamicItem);
         put(result);
      }

      return result;
   }
}

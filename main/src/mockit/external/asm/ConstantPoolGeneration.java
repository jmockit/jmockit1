package mockit.external.asm;

import javax.annotation.*;

import mockit.external.asm.Handle.*;
import mockit.internal.util.*;
import static mockit.external.asm.ConstantPoolItemType.*;
import static mockit.internal.util.ClassLoad.OBJECT;

/**
 * Allows the constant pool for a classfile to be created from scratch, when that classfile itself is being generated or
 * modified from an existing class file.
 */
final class ConstantPoolGeneration
{
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
    * A reusable key used to look for items in the {@link #items} hash table.
    */
   @Nonnull private final ReferenceItem key;

   /**
    * A reusable key used to look for items in the {@link #items} hash table.
    */
   @Nonnull private final ReferenceItem key2;

   /**
    * A reusable key used to look for items in the {@link #items} hash table.
    */
   @Nonnull private final ReferenceItem key3;

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
   @Nonnull private final InvokeDynamicItem reusableInvokeDynamicItem;

   /**
    * A reusable key used to look for items in the {@link #items} hash table.
    */
   @Nonnull private final ReferenceItem key4;

   /**
    * Index of the next item to be added in the constant pool.
    */
   @Nonnegative private int index;

   /**
    * A type table used to temporarily store internal names that will not necessarily be stored in the constant pool.
    * This type table is used by the control flow and data flow analysis algorithm used to compute stack map frames
    * from scratch. This array associates to each index <tt>i</tt> the Item whose index is <tt>i</tt>. All Item objects
    * stored in this array are also stored in the {@link #items} hash table. These two arrays allow to retrieve an Item
    * from its index or, conversely, to get the index of an Item from its value. Each Item stores an internal name in
    * its {@link Item#strVal1} field.
    */
   private Item[] typeTable;

   /**
    * Number of elements in the {@link #typeTable} array.
    */
   private short typeCount;

   ConstantPoolGeneration() {
      pool = new ByteVector();
      items = new Item[256];
      threshold = (int) (0.75d * items.length);
      key = new ReferenceItem(0);
      key2 = new ReferenceItem(0);
      key3 = new ReferenceItem(0);
      reusableIntItem = new IntItem(0);
      reusableLongItem = new LongItem(0);
      reusableFloatItem = new FloatItem(0);
      reusableDoubleItem = new DoubleItem(0);
      reusableInvokeDynamicItem = new InvokeDynamicItem(0);
      key4 = new ReferenceItem(0);
      index = 1;
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
      key.set(UTF8, value, null, null);
      Item result = get(key);

      if (result == null) {
         pool.putByte(UTF8).putUTF8(value);
         result = new Item(index++, key);
         put(result);
      }

      return result.index;
   }

   /**
    * Adds a class reference to the constant pool of the class being built.
    * Does nothing if the constant pool already contains a similar item.
    *
    * @param value the internal name of the class.
    * @return a new or already existing class reference item.
    */
   @Nonnull
   Item newClassItem(@Nonnull String value) {
      return newItem(CLASS, value);
   }

   @Nonnull
   private Item newItem(int type, @Nonnull String value) {
      key2.set(type, value, null, null);
      Item result = get(key2);

      if (result == null) {
         int itemIndex = newUTF8(value);
         pool.put12(type, itemIndex);
         result = new Item(index++, key2);
         put(result);
      }

      return result;
   }

   /**
    * Adds a class reference to the constant pool of the class being built.
    * Does nothing if the constant pool already contains a similar item.
    *
    * @param value the internal name of the class.
    * @return the index of a new or already existing class reference item.
    */
   @Nonnegative
   int newClass(@Nonnull String value) {
      return newItem(CLASS, value).index;
   }

   /**
    * Adds a method type reference to the constant pool of the class being built.
    * Does nothing if the constant pool already contains a similar item.
    *
    * @param methodDesc method descriptor of the method type.
    * @return a new or already existing method type reference item.
    */
   @Nonnull
   Item newMethodTypeItem(@Nonnull String methodDesc) {
      return newItem(MTYPE, methodDesc);
   }

   /**
    * Adds a handle to the constant pool of the class being built.
    * Does nothing if the constant pool already contains a similar item.
    *
    * @return a new or an already existing method type reference item.
    */
   @Nonnull
   Item newHandleItem(@Nonnull Handle handle) {
      int tag = handle.tag;
      key4.set(HANDLE_BASE + tag, handle.owner, handle.name, handle.desc);
      Item result = get(key4);

      if (result == null) {
         Item item;

         if (tag <= Handle.Tag.PUTSTATIC) {
            item = newFieldItem(handle.owner, handle.name, handle.desc);
         }
         else {
            boolean itf = tag == Tag.INVOKEINTERFACE;
            item = newMethodItem(handle.owner, handle.name, handle.desc, itf);
         }

         pool.put11(HANDLE, tag).putShort(item.index);

         result = new Item(index++, key4);
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
   Item newFieldItem(@Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
      return newItem(FIELD, owner, name, desc);
   }

   @Nonnull
   private Item newItem(int type, @Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
      key3.set(type, owner, name, desc);
      Item result = get(key3);

      if (result == null) {
         int ownerItemIndex = newClass(owner);
         int nameAndTypeItemIndex = newNameType(name, desc);
         put122(type, ownerItemIndex, nameAndTypeItemIndex);
         result = new Item(index++, key3);
         put(result);
      }

      return result;
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
   Item newMethodItem(@Nonnull String owner, @Nonnull String name, @Nonnull String desc, boolean itf) {
      return newItem(itf ? IMETH : METH, owner, name, desc);
   }

   /**
    * Adds an integer to the constant pool of the class being built.
    * Does nothing if the constant pool already contains a similar item.
    *
    * @param value the int value.
    * @return a new or already existing int item.
    */
   @Nonnull
   Item newInteger(int value) {
      reusableIntItem.set(value);
      Item result = get(reusableIntItem);

      if (result == null) {
         pool.putByte(INT).putInt(value);
         result = new Item(index++, reusableIntItem);
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
   Item newFloat(float value) {
      reusableFloatItem.set(value);
      Item result = get(reusableFloatItem);

      if (result == null) {
         pool.putByte(FLOAT).putInt(reusableFloatItem.intVal);
         result = new Item(index++, reusableFloatItem);
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
   Item newLong(long value) {
      reusableLongItem.set(value);
      Item result = get(reusableLongItem);

      if (result == null) {
         pool.putByte(LONG).putLong(value);
         result = new Item(index, reusableLongItem);
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
   Item newDouble(double value) {
      reusableDoubleItem.set(value);
      Item result = get(reusableDoubleItem);

      if (result == null) {
         pool.putByte(DOUBLE).putLong(reusableDoubleItem.longVal);
         result = new Item(index, reusableDoubleItem);
         index += 2;
         put(result);
      }

      return result;
   }

   /**
    * Adds a string to the constant pool of the class being built.
    * Does nothing if the constant pool already contains a similar item.
    *
    * @param value the String value.
    * @return a new or already existing string item.
    */
   @Nonnull
   Item newString(@Nonnull String value) {
      return newItem(STR, value);
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
      key2.set(NAME_TYPE, name, desc, null);
      Item result = get(key2);

      if (result == null) {
         int nameItemIndex = newUTF8(name);
         int descItemIndex = newUTF8(desc);
         put122(NAME_TYPE, nameItemIndex, descItemIndex);
         result = new Item(index++, key2);
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
   int addType(@Nonnull String type) {
      key.set(Item.SpecialType.NORMAL, type, null, null);
      Item result = get(key);

      if (result == null) {
         result = addType();
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
      key.type = Item.SpecialType.UNINIT;
      key.intVal = offset;
      key.strVal1 = type;
      key.hashCode = 0x7FFFFFFF & (Item.SpecialType.UNINIT + type.hashCode() + offset);
      Item result = get(key);

      if (result == null) {
         result = addType();
      }

      return result.index;
   }

   /**
    * Creates and adds a new Item to {@link #typeTable}.
    *
    * @return the added Item, which is a new Item instance with the same value as {@link #key}.
    */
   @Nonnull
   private Item addType() {
      typeCount++;

      Item result = new Item(typeCount, key);
      put(result);

      if (typeTable == null) {
         typeTable = new Item[16];
      }

      if (typeCount == typeTable.length) {
         Item[] newTable = new Item[2 * typeTable.length];
         System.arraycopy(typeTable, 0, newTable, 0, typeTable.length);
         typeTable = newTable;
      }

      typeTable[typeCount] = result;
      return result;
   }

   /**
    * Returns the index of the common super type of the two given types. This method calls {@link #getCommonSuperClass}
    * and caches the result in the {@link #items} hash table to speedup future calls with the same parameters.
    *
    * @param type1 index of an internal name in {@link #typeTable}.
    * @param type2 index of an internal name in {@link #typeTable}.
    * @return the index of the common super type of the two given types.
    */
   int getMergedType(int type1, int type2) {
      key2.type = Item.SpecialType.MERGED;
      key2.longVal = type1 | ((long) type2 << 32);
      key2.hashCode = 0x7FFFFFFF & (Item.SpecialType.MERGED + type1 + type2);
      Item result = get(key2);

      if (result == null) {
         String t = getInternalName(type1);
         String u = getInternalName(type2);
         String commonSuperClass = getCommonSuperClass(t, u);
         key2.intVal = addType(commonSuperClass);
         result = new Item((short) 0, key2);
         put(result);
      }

      return result.intVal;
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
   private static String getCommonSuperClass(String type1, String type2) {
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

   @Nullable
   Item getItem(int hashCode) { return items[hashCode % items.length]; }

   /**
    * Returns the constant pool's hash table item which is equal to the given item.
    *
    * @param key a constant pool item.
    * @return the constant pool's hash table item which is equal to the given item, or <tt>null</tt> if there is no
    * such item.
    */
   @Nullable
   private Item get(@Nonnull Item key) {
      Item item = getItem(key.hashCode);
      int keyType = key.type;

      while (item != null && (item.type != keyType || !key.isEqualTo(item))) {
         item = item.next;
      }

      return item;
   }

   /**
    * Puts the given item in the constant pool's hash table. The hash table <i>must</i> not already contains this item.
    *
    * @param item the item to be added to the constant pool's hash table.
    */
   void put(@Nonnull Item item) {
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

      int index = item.hashCode % items.length;
      item.next = items[index];
      items[index] = item;
   }

   private static void put(@Nonnull Item[] newItems, @Nullable Item item) {
      while (item != null) {
         int index = item.hashCode % newItems.length;
         Item k = item.next;
         item.next = newItems[index];
         newItems[index] = item;
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

   String getInternalName(@Nonnegative int index) { return typeTable[index].strVal1; }
   int getIntegerItemValue(@Nonnegative int index) { return typeTable[index].intVal; }

   @Nonnull
   Item createInvokeDynamicConstant(@Nonnull String name, @Nonnull String desc, @Nonnegative int bsmIndex) {
      reusableInvokeDynamicItem.set(name, desc, bsmIndex);
      Item result = get(reusableInvokeDynamicItem);

      if (result == null) {
         put122(INDY, bsmIndex, newNameType(name, desc));
         result = new Item(index++, reusableInvokeDynamicItem);
         put(result);
      }

      return result;
   }
}

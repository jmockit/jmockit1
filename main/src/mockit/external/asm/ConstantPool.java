package mockit.external.asm;

import mockit.external.asm.Handle.*;
import mockit.internal.util.*;
import static mockit.external.asm.ConstantPoolItemType.*;
import static mockit.internal.util.ClassLoad.OBJECT;

final class ConstantPool
{
   /**
    * Index of the next item to be added in the constant pool.
    */
   int index;

   /**
    * The constant pool of this class.
    */
   private final ByteVector pool;

   /**
    * The constant pool's hash table data.
    */
   Item[] items;

   /**
    * The threshold of the constant pool's hash table.
    */
   private int threshold;

   /**
    * A reusable key used to look for items in the {@link #items} hash table.
    */
   private final Item key;

   /**
    * A reusable key used to look for items in the {@link #items} hash table.
    */
   private final Item key2;

   /**
    * A reusable key used to look for items in the {@link #items} hash table.
    */
   final Item key3;

   /**
    * A reusable key used to look for items in the {@link #items} hash table.
    */
   private final Item key4;

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

   ConstantPool() {
      index = 1;
      pool = new ByteVector();
      items = new Item[256];
      threshold = (int) (0.75d * items.length);
      key = new Item();
      key2 = new Item();
      key3 = new Item();
      key4 = new Item();
   }

   /**
    * Adds an UTF8 string to the constant pool of the class being built.
    * Does nothing if the constant pool already contains a similar item.
    *
    * @param value the String value.
    * @return the index of a new or already existing UTF8 item.
    */
   int newUTF8(String value) {
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
   Item newClassItem(String value) {
      key2.set(CLASS, value, null, null);
      Item result = get(key2);

      if (result == null) {
         pool.put12(CLASS, newUTF8(value));
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
   int newClass(String value) {
      return newClassItem(value).index;
   }

   /**
    * Adds a method type reference to the constant pool of the class being built.
    * Does nothing if the constant pool already contains a similar item.
    *
    * @param methodDesc method descriptor of the method type.
    * @return a new or already existing method type reference item.
    */
   Item newMethodTypeItem(String methodDesc) {
      key2.set(MTYPE, methodDesc, null, null);
      Item result = get(key2);

      if (result == null) {
         int itemIndex = newUTF8(methodDesc);
         pool.put12(MTYPE, itemIndex);
         result = new Item(index++, key2);
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
   Item newHandleItem(Handle handle) {
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
   Item newFieldItem(String owner, String name, String desc) {
      key3.set(FIELD, owner, name, desc);
      Item result = get(key3);

      if (result == null) {
         put122(FIELD, newClass(owner), newNameType(name, desc));
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
   Item newMethodItem(String owner, String name, String desc, boolean itf) {
      int type = itf ? IMETH : METH;
      key3.set(type, owner, name, desc);
      Item result = get(key3);

      if (result == null) {
         put122(type, newClass(owner), newNameType(name, desc));
         result = new Item(index++, key3);
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
   Item newInteger(int value) {
      key.set(value);
      Item result = get(key);

      if (result == null) {
         pool.putByte(INT).putInt(value);
         result = new Item(index++, key);
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
   Item newFloat(float value) {
      key.set(value);
      Item result = get(key);

      if (result == null) {
         pool.putByte(FLOAT).putInt(key.intVal);
         result = new Item(index++, key);
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
   Item newLong(long value) {
      key.set(value);
      Item result = get(key);

      if (result == null) {
         pool.putByte(LONG).putLong(value);
         result = new Item(index, key);
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
   Item newDouble(double value) {
      key.set(value);
      Item result = get(key);

      if (result == null) {
         pool.putByte(DOUBLE).putLong(key.longVal);
         result = new Item(index, key);
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
   Item newString(String value) {
      key2.set(STR, value, null, null);
      Item result = get(key2);

      if (result == null) {
         pool.put12(STR, newUTF8(value));
         result = new Item(index++, key2);
         put(result);
      }

      return result;
   }

   /**
    * Adds a name and type to the constant pool of the class being build.
    * Does nothing if the constant pool already contains a similar item.
    *
    * @param name a name.
    * @param desc a type descriptor.
    * @return the index of a new or already existing name and type item.
    */
   private int newNameType(String name, String desc) {
      return newNameTypeItem(name, desc).index;
   }

   /**
    * Adds a name and type to the constant pool of the class being build.
    * Does nothing if the constant pool already contains a similar item.
    *
    * @param name a name.
    * @param desc a type descriptor.
    * @return a new or already existing name and type item.
    */
   Item newNameTypeItem(String name, String desc) {
      key2.set(NAME_TYPE, name, desc, null);
      Item result = get(key2);

      if (result == null) {
         put122(NAME_TYPE, newUTF8(name), newUTF8(desc));
         result = new Item(index++, key2);
         put(result);
      }

      return result;
   }

   /**
    * Adds the given internal name to {@link #typeTable} and returns its index.
    * Does nothing if the type table already contains this internal name.
    *
    * @param type the internal name to be added to the type table.
    * @return the index of this internal name in the type table.
    */
   int addType(String type) {
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
   int addUninitializedType(String type, int offset) {
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
    * Adds the given Item to {@link #typeTable}.
    *
    * @return the added Item, which a new Item instance with the same value as the given Item.
    */
   private Item addType() {
      ++typeCount;
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
         key2.intVal = addType(getCommonSuperClass(t, u));
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
   private String getCommonSuperClass(String type1, String type2) {
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

   /**
    * Returns the constant pool's hash table item which is equal to the given item.
    *
    * @param key a constant pool item.
    * @return the constant pool's hash table item which is equal to the given item, or <tt>null</tt> if there is no
    * such item.
    */
   Item get(Item key) {
      Item item = items[key.hashCode % items.length];

      while (item != null && (item.type != key.type || !key.isEqualTo(item))) {
         item = item.next;
      }

      return item;
   }

   /**
    * Puts the given item in the constant pool's hash table. The hash table <i>must</i> not already contains this item.
    *
    * @param item the item to be added to the constant pool's hash table.
    */
   void put(Item item) {
      if (index + typeCount > threshold) {
         int ll = items.length;
         int nl = ll * 2 + 1;
         Item[] newItems = new Item[nl];

         for (int l = ll - 1; l >= 0; --l) {
            Item j = items[l];

            while (j != null) {
               int index = j.hashCode % newItems.length;
               Item k = j.next;
               j.next = newItems[index];
               newItems[index] = j;
               j = k;
            }
         }

         items = newItems;
         threshold = (int) (nl * 0.75);
      }

      int index = item.hashCode % items.length;
      item.next = items[index];
      items[index] = item;
   }

   /**
    * Puts one byte and two shorts into the constant pool.
    *
    * @param b  a byte.
    * @param s1 a short.
    * @param s2 another short.
    */
   void put122(int b, int s1, int s2) {
      pool.put12(b, s1).putShort(s2);
   }

   int getSize() { return pool.length; }

   void checkConstantPoolMaxSize() {
      if (index > 0xFFFF) {
         throw new RuntimeException("Class file too large!");
      }
   }

   void put(ByteVector out) {
      out.putShort(index).putByteVector(pool);
   }

   void copy(byte[] b, int off, int header, Item[] items) {
      pool.putByteArray(b, off, header - off);
      this.items = items;

      int ll = items.length;
      threshold = (int) (0.75d * ll);
      index = ll;
   }

   String getInternalName(int index) { return typeTable[index].strVal1; }
   int getIntegerItemValue(int index) { return typeTable[index].intVal; }
}

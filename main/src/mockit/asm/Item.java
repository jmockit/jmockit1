package mockit.asm;

import javax.annotation.*;

/**
 * A constant pool item of a given {@linkplain Type type}.
 */
abstract class Item
{
   /**
    * Constants for <a href="https://docs.oracle.com/javase/specs/jvms/se10/html/jvms-4.html#jvms-4.4-150">types of items</a> in the
    * constant pool of a class.
    */
   interface Type {
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
      int HANDLE    = 15; // CONSTANT_MethodHandle
      int MTYPE     = 16; // CONSTANT_MethodType
      int INDY      = 18; // CONSTANT_InvokeDynamic

      /**
       * The base value for all CONSTANT_MethodHandle constant pool items.
       * Internally, there are 9 variations of CONSTANT_MethodHandle items, which are stored into 9 different items, of types 21 to 29.
       */
      int HANDLE_BASE = 20;

      /**
       * The type of BootstrapMethod items.
       * These items are stored in a special class attribute named "BootstrapMethods" and not in the constant pool.
       */
      int BSM = 33;
   }

   /**
    * Index of this item in the constant pool.
    */
   @Nonnegative final int index;

   /**
    * {@link Type Type} of this constant pool item.
    * <p/>
    * MethodHandle variations are stored using a range of 9 values from {@link Type#HANDLE_BASE HANDLE_BASE} + 1 to
    * {@link Type#HANDLE_BASE HANDLE_BASE} + 9.
    * <p/>
    * Special Item types are used for items that are stored in the {@link ConstantPoolGeneration#typeTable}, instead of the constant pool,
    * in order to avoid clashes with normal constant pool items in the constant pool's hash table.
    * These special item types are defined in {@link TypeTableItem.SpecialType}.
    */
   @Nonnegative int type;

   /**
    * The hash code value of this constant pool item.
    */
   @Nonnegative private int hashCode;

   /**
    * Link to another constant pool item, used for collision lists in the constant pool's hash table.
    */
   @Nullable private Item next;

   /**
    * Initializes an Item for a constant pool element at the given position.
    *
    * @param index index of the item
    */
   Item(@Nonnegative int index) { this.index = index; }

   /**
    * Initializes this item as a copy of a given item.
    *
    * @param index index of the new item
    * @param item the item to be copied into this item
    */
   Item(@Nonnegative int index, @Nonnull Item item) {
      this.index = index;
      type = item.type;
      hashCode = item.hashCode;
   }

   /**
    * Returns the {@link #index} of this item.
    */
   @Nonnegative
   int getIndex() { return index; }

   /**
    * Returns the {@link #type} of this item.
    */
   @Nonnegative
   int getType() { return type; }

   /**
    * Returns the {@link #hashCode} value.
    */
   @Nonnegative
   final int getHashCode() { return hashCode; }

   final void setHashCode(int valuesHashCode) {
      hashCode = 0x7FFFFFFF & (type + valuesHashCode);
   }

   /**
    * Returns the {@link #next} item, if any.
    */
   @Nullable
   Item getNext() { return next; }

   final void setNext(@Nonnull Item[] items) {
      int indexOfNextItem = hashCode % items.length;
      next = items[indexOfNextItem];
      items[indexOfNextItem] = this;
   }

   /**
    * Indicates if the given item is equal to this one, <em>assuming that the two items have the same {@link #type}</em>.
    *
    * @param item the item to be compared to this one (both items must have the same {@link #type})
    * @return <tt>true</tt> if the given item is equal to this one, <tt>false</tt> otherwise
    */
   abstract boolean isEqualTo(@Nonnull Item item);
}

package mockit.asm.constantPool;

import javax.annotation.*;

import mockit.asm.jvmConstants.*;

/**
 * A constant pool item of a given {@linkplain ConstantPoolTypes type}.
 */
public abstract class Item
{
   /**
    * Index of this item in the constant pool.
    */
   @Nonnegative public final int index;

   /**
    * {@link ConstantPoolTypes Type} of this constant pool item.
    * <p>
    * MethodHandle variations are stored using a range of 9 values from {@link ConstantPoolTypes#HANDLE_BASE HANDLE_BASE} + 1 to
    * {@link ConstantPoolTypes#HANDLE_BASE HANDLE_BASE} + 9.
    * <p>
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
    * Returns the {@link #type} of this item.
    */
   @Nonnegative
   public int getType() { return type; }

   /**
    * Returns the {@link #hashCode} value.
    */
   @Nonnegative
   public final int getHashCode() { return hashCode; }

   final void setHashCode(int valuesHashCode) {
      hashCode = 0x7FFFFFFF & (type + valuesHashCode);
   }

   /**
    * Returns the {@link #next} item, if any.
    */
   @Nullable
   public Item getNext() { return next; }

   public final void setNext(@Nonnull Item[] items) {
      int indexOfNextItem = hashCode % items.length;
      next = items[indexOfNextItem];
      items[indexOfNextItem] = this;
   }

   /**
    * Indicates if the given item is equal to this one, <em>assuming that the two items have the same {@link #type}</em>.
    *
    * @param item the item to be compared to this one (both items must have the same {@link #type})
    * @return <code>true</code> if the given item is equal to this one, <code>false</code> otherwise
    */
   abstract boolean isEqualTo(@Nonnull Item item);
}

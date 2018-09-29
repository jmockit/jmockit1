package mockit.asm.controlFlow;

import javax.annotation.*;

import mockit.asm.constantPool.*;
import mockit.asm.jvmConstants.*;
import mockit.asm.types.*;
import mockit.asm.util.*;

/**
 * Writes the "StackMapTable" method attribute (or "StackMap" for classfiles older than Java 6).
 */
public final class StackMapTableWriter extends AttributeWriter
{
   /**
    * Constants that identify how many locals and stack items a frame has, with respect to its previous frame.
    */
   interface LocalsAndStackItemsDiff {
      /**
       * Same locals as the previous frame, number of stack items is zero.
       */
      int SAME_FRAME = 0; // to 63 (0-3f)

      /**
       * Same locals as the previous frame, number of stack items is 1.
       */
      int SAME_LOCALS_1_STACK_ITEM_FRAME = 64; // to 127 (40-7f)

      /**
       * Same locals as the previous frame, number of stack items is 1. Offset is bigger then 63.
       */
      int SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED = 247; // f7

      /**
       * Current locals are the same as the locals in the previous frame, except that the k last locals are absent.
       * The value of k is given by the formula 251-frame_type.
       */
      int CHOP_FRAME = 248; // to 250 (f8-fA)

      /**
       * Same locals as the previous frame, number of stack items is zero. Offset is bigger then 63.
       */
      int SAME_FRAME_EXTENDED = 251; // fb

      /**
       * Current locals are the same as the locals in the previous frame, except that k additional locals are defined.
       * The value of k is given by the formula frame_type-251.
       */
      int APPEND_FRAME = 252; // to 254 // fc-fe

      /**
       * Full frame.
       */
      int FULL_FRAME = 255; // ff
   }

   private final boolean java6OrNewer;

   /**
    * Maximum stack size of this method.
    */
   @Nonnegative private int maxStack;

   /**
    * Maximum number of local variables for this method.
    */
   @Nonnegative private int maxLocals;

   /**
    * Number of stack map frames in the StackMapTable attribute.
    */
   @Nonnegative private int frameCount;

   /**
    * The StackMapTable attribute.
    */
   private ByteVector stackMap;

   /**
    * The last frame that was written in the StackMapTable attribute.
    *
    * @see #frameDefinition
    */
   private int[] previousFrame;

   /**
    * The current stack map frame.
    * <p/>
    * The first element contains the offset of the instruction to which the frame corresponds (frameDefinition[0] = offset), the second
    * element is the number of locals (frameDefinition[1] = nLocal), and the third one is the number of stack elements (frameDefinition[2]
    * = nStack).
    * The local variables start at index 3 (frameDefinition[3 to 3+nLocal-1]) and are followed by the operand stack values
    * (frameDefinition[3+nLocal...]).
    * <p/>
    * All types are encoded as integers, with the same format as the one used in {@link Label}, but limited to BASE types.
    */
   private int[] frameDefinition;

   /**
    * The current index in {@link #frameDefinition}, when writing new values into the array.
    */
   @Nonnegative private int frameIndex;

   public StackMapTableWriter(@Nonnull ConstantPoolGeneration cp, boolean java6OrNewer, int methodAccess, @Nonnull String methodDesc) {
      super(cp);
      this.java6OrNewer = java6OrNewer;

      int size = JavaType.getArgumentsAndReturnSizes(methodDesc) >> 2;

      if ((methodAccess & Access.STATIC) != 0) {
         size--;
      }

      maxLocals = size;
   }

   public void setMaxStack(@Nonnegative int maxStack) { this.maxStack = maxStack; }

   public void updateMaxLocals(@Nonnegative int n) {
      if (n > maxLocals) {
         maxLocals = n;
      }
   }

   public void putMaxStackAndLocals(@Nonnull ByteVector out) {
      out.putShort(maxStack).putShort(maxLocals);
   }

   @Nonnegative private int getInstructionOffset() { return frameDefinition[0]; }
   private void setInstructionOffset(@Nonnegative int offset) { frameDefinition[0] = offset; }

   @Nonnegative private int getNumLocals() { return frameDefinition[1]; }
   private void setNumLocals(@Nonnegative int numLocals) { frameDefinition[1] = numLocals; }

   @Nonnegative private int getStackSize() { return frameDefinition[2]; }
   private void setStackSize(@Nonnegative int stackSize) { frameDefinition[2] = stackSize; }

   private void writeFrameDefinition(@Nonnegative int value) { frameDefinition[frameIndex++] = value; }

   public boolean hasStackMap() { return stackMap != null; }

   /**
    * Starts the visit of a stack map frame.
    * Sets {@link #frameIndex} to the index of the next element to be written in this frame.
    *
    * @param offset the offset of the instruction to which the frame corresponds.
    * @param nLocals the number of local variables in the frame.
    * @param nStack the number of stack elements in the frame.
    */
   private void startFrame(@Nonnegative int offset, @Nonnegative int nLocals, @Nonnegative int nStack) {
      int n = 3 + nLocals + nStack;

      if (frameDefinition == null || frameDefinition.length < n) {
         frameDefinition = new int[n];
      }

      setInstructionOffset(offset);
      setNumLocals(nLocals);
      setStackSize(nStack);
      frameIndex = 3;
   }

   /**
    * Checks if the visit of the current {@link #frameDefinition frame} is finished, and if yes, write it in the StackMapTable attribute.
    */
   private void endFrame() {
      if (previousFrame != null) { // do not write the first frame
         if (stackMap == null) {
            setAttribute(java6OrNewer ? "StackMapTable" : "StackMap");
            stackMap = new ByteVector();
         }

         writeFrame();
         frameCount++;
      }

      previousFrame = frameDefinition;
      frameDefinition = null;
   }

   /**
    * Compress and writes the current {@link #frameDefinition frame} in the StackMapTable attribute.
    */
   private void writeFrame() {
      int currentLocalsSize = getNumLocals();
      int currentStackSize = getStackSize();

      if (java6OrNewer) {
         writeFrameForJava6OrNewer(currentLocalsSize, currentStackSize);
      }
      else {
         writeFrameForOldVersionOfJava(currentLocalsSize, currentStackSize);
      }
   }

   private void writeFrameForOldVersionOfJava(@Nonnegative int localsSize, @Nonnegative int stackSize) {
      int instructionOffset = getInstructionOffset();
      writeFrame(instructionOffset, localsSize, stackSize);
   }

   private void writeFullFrame(@Nonnegative int instructionOffset, @Nonnegative int localsSize, @Nonnegative int stackSize) {
      stackMap.putByte(LocalsAndStackItemsDiff.FULL_FRAME);
      writeFrame(instructionOffset, localsSize, stackSize);
   }

   private void writeFrame(@Nonnegative int instructionOffset, @Nonnegative int localsSize, @Nonnegative int stackSize) {
      stackMap.putShort(instructionOffset);

      stackMap.putShort(localsSize);
      int lastTypeIndex = 3 + localsSize;
      writeFrameTypes(3, lastTypeIndex);

      stackMap.putShort(stackSize);
      writeFrameTypes(lastTypeIndex, lastTypeIndex + stackSize);
   }

   private void writeFrameForJava6OrNewer(@Nonnegative int currentLocalsSize, @Nonnegative int currentStackSize) {
      @Nonnegative int previousLocalsSize = previousFrame[1];
      int k = currentStackSize == 0 ? currentLocalsSize - previousLocalsSize : 0;
      @Nonnegative int delta = getDelta();
      int type = LocalsAndStackItemsDiff.FULL_FRAME;

      if (currentStackSize == 0) {
         //noinspection SwitchStatementWithoutDefaultBranch
         switch (k) {
            case -3: case -2: case -1:
               type = LocalsAndStackItemsDiff.CHOP_FRAME;
               previousLocalsSize = currentLocalsSize;
               break;
            case 0:
               type = delta < 64 ? LocalsAndStackItemsDiff.SAME_FRAME : LocalsAndStackItemsDiff.SAME_FRAME_EXTENDED;
               break;
            case 1: case 2: case 3:
               type = LocalsAndStackItemsDiff.APPEND_FRAME;
         }
      }
      else if (currentLocalsSize == previousLocalsSize && currentStackSize == 1) {
         type = delta < 63 ?
            LocalsAndStackItemsDiff.SAME_LOCALS_1_STACK_ITEM_FRAME :
            LocalsAndStackItemsDiff.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED;
      }

      type = chooseTypeAsFullFrameIfApplicable(previousLocalsSize, type);

      switch (type) {
         case LocalsAndStackItemsDiff.SAME_FRAME:
            stackMap.putByte(delta);
            break;
         case LocalsAndStackItemsDiff.SAME_LOCALS_1_STACK_ITEM_FRAME:
            writeFrameWithSameLocalsAndOneStackItem(currentLocalsSize, delta);
            break;
         case LocalsAndStackItemsDiff.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED:
            writeExtendedFrameWithSameLocalsAndOneStackItem(currentLocalsSize, delta);
            break;
         case LocalsAndStackItemsDiff.SAME_FRAME_EXTENDED:
            writeFrameWithSameLocalsAndZeroStackItems(0, delta);
            break;
         case LocalsAndStackItemsDiff.CHOP_FRAME:
            writeFrameWithSameLocalsAndZeroStackItems(k, delta);
            break;
         case LocalsAndStackItemsDiff.APPEND_FRAME:
            writeAppendedFrame(currentLocalsSize, previousLocalsSize, k, delta);
            break;
         default: // LocalsAndStackItemsDiff.FULL_FRAME
            writeFullFrame(delta, currentLocalsSize, currentStackSize);
      }
   }

   private void writeFrameWithSameLocalsAndOneStackItem(@Nonnegative int localsSize, @Nonnegative int delta) {
      stackMap.putByte(LocalsAndStackItemsDiff.SAME_LOCALS_1_STACK_ITEM_FRAME + delta);
      writeFrameTypes(3 + localsSize, 4 + localsSize);
   }

   private void writeFrameWithSameLocalsAndZeroStackItems(int k, @Nonnegative int delta) {
      stackMap.putByte(LocalsAndStackItemsDiff.SAME_FRAME_EXTENDED + k).putShort(delta);
   }

   private void writeExtendedFrameWithSameLocalsAndOneStackItem(@Nonnegative int localsSize, @Nonnegative int delta) {
      stackMap.putByte(LocalsAndStackItemsDiff.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED).putShort(delta);
      writeFrameTypes(3 + localsSize, 4 + localsSize);
   }

   private void writeAppendedFrame(@Nonnegative int currentLocalsSize, @Nonnegative int previousLocalsSize, int k, @Nonnegative int delta) {
      stackMap.putByte(LocalsAndStackItemsDiff.SAME_FRAME_EXTENDED + k).putShort(delta);
      writeFrameTypes(3 + previousLocalsSize, 3 + currentLocalsSize);
   }

   @Nonnegative
   private int getDelta() {
      int offset = getInstructionOffset();
      return frameCount == 0 ? offset : offset - previousFrame[0] - 1;
   }

   @Nonnegative
   private int chooseTypeAsFullFrameIfApplicable(@Nonnegative int localsSize, @Nonnegative int type) {
      if (type != LocalsAndStackItemsDiff.FULL_FRAME) {
         // Verify if locals are the same.
         int l = 3;

         for (int j = 0; j < localsSize; j++) {
            if (frameDefinition[l] != previousFrame[l]) {
               return LocalsAndStackItemsDiff.FULL_FRAME;
            }

            l++;
         }
      }

      return type;
   }

   /**
    * Writes some types of the current {@link #frameDefinition frame} into the StackMapTable attribute.
    * This method converts types from the format used in {@link Label} to the format used in StackMapTable attributes.
    * In particular, it converts type table indexes to constant pool indexes.
    *
    * @param start index of the first type in {@link #frameDefinition} to write
    * @param end index of last type in {@link #frameDefinition} to write (exclusive)
    */
   private void writeFrameTypes(@Nonnegative int start, @Nonnegative int end) {
      for (int i = start; i < end; i++) {
         int type = frameDefinition[i];
         int dimensions = type & FrameTypeMask.DIM;

         if (dimensions == 0) {
            writeFrameOfRegularType(type);
         }
         else {
            writeFrameOfArrayType(dimensions, type);
         }
      }
   }

   private void writeFrameOfRegularType(@Nonnegative int type) {
      int typeTableIndex = type & FrameTypeMask.BASE_VALUE;

      switch (type & FrameTypeMask.BASE_KIND) {
         case FrameTypeMask.OBJECT:
            String classDesc = cp.getInternalName(typeTableIndex);
            int classDescIndex = cp.newClass(classDesc);
            stackMap.putByte(7).putShort(classDescIndex);
            break;
         case FrameTypeMask.UNINITIALIZED:
            UninitializedTypeTableItem uninitializedItemValue = cp.getUninitializedItemValue(typeTableIndex);
            int typeDesc = uninitializedItemValue.getOffset();
            stackMap.putByte(8).putShort(typeDesc);
            break;
         default:
            stackMap.putByte(typeTableIndex);
      }
   }

   private void writeFrameOfArrayType(@Nonnegative int arrayDimensions, @Nonnegative int arrayElementType) {
      StringBuilder sb = new StringBuilder();
      writeDimensionsIntoArrayDescriptor(sb, arrayDimensions);

      if ((arrayElementType & FrameTypeMask.BASE_KIND) == FrameTypeMask.OBJECT) {
         String arrayElementTypeDesc = cp.getInternalName(arrayElementType & FrameTypeMask.BASE_VALUE);
         sb.append('L').append(arrayElementTypeDesc).append(';');
      }
      else {
         char typeCode = getTypeCodeForArrayElements(arrayElementType);
         sb.append(typeCode);
      }

      String arrayElementTypeDesc = sb.toString();
      int typeDescIndex = cp.newClass(arrayElementTypeDesc);
      stackMap.putByte(7).putShort(typeDescIndex);
   }

   private static void writeDimensionsIntoArrayDescriptor(@Nonnull StringBuilder sb, @Nonnegative int arrayDimensions) {
      arrayDimensions >>= 28;

      while (arrayDimensions-- > 0) {
         sb.append('[');
      }
   }

   private static char getTypeCodeForArrayElements(@Nonnegative int arrayElementType) {
      switch (arrayElementType & 0xF) {
         case  1: return 'I';
         case  2: return 'F';
         case  3: return 'D';
         case  9: return 'Z';
         case 10: return 'B';
         case 11: return 'C';
         case 12: return 'S';
         default: return 'J';
      }
   }

   /**
    * Creates and visits the first (implicit) frame.
    */
   public void createAndVisitFirstFrame(@Nonnull Frame frame, @Nonnull String classDesc, @Nonnull String methodDesc, int methodAccess) {
      JavaType[] args = JavaType.getArgumentTypes(methodDesc);
      frame.initInputFrame(classDesc, methodAccess, args, maxLocals);
      visitFrame(frame);
   }

   /**
    * Visits a frame that has been computed from scratch.
    */
   public void visitFrame(@Nonnull Frame frame) {
      int[] locals = frame.inputLocals;
      int nLocal = computeNumberOfLocals(locals);

      int[] stacks = frame.inputStack;
      int nStack = computeStackSize(stacks);

      startFrame(frame.owner.position, nLocal, nStack);
      putLocalsOrStackElements(locals, nLocal);
      putLocalsOrStackElements(stacks, nStack);
      endFrame();
   }

   /**
    * Computes the number of locals (ignores TOP types that are just after a LONG or a DOUBLE, and all trailing TOP types).
    */
   @Nonnegative
   private static int computeNumberOfLocals(@Nonnull int[] locals) {
      int nLocal = 0;
      int nTop = 0;

      for (int i = 0; i < locals.length; i++) {
         int t = locals[i];

         if (t == FrameTypeMask.TOP) {
            nTop++;
         }
         else {
            nLocal += nTop + 1;
            nTop = 0;
         }

         if (t == FrameTypeMask.LONG || t == FrameTypeMask.DOUBLE) {
            i++;
         }
      }

      return nLocal;
   }

   /**
    * Computes the stack size (ignores TOP types that are just after a LONG or a DOUBLE).
    */
   @Nonnegative
   private static int computeStackSize(@Nonnull int[] stacks) {
      int nStack = 0;

      for (int i = 0; i < stacks.length; i++) {
         int t = stacks[i];
         nStack++;

         if (t == FrameTypeMask.LONG || t == FrameTypeMask.DOUBLE) {
            i++;
         }
      }

      return nStack;
   }

   private void putLocalsOrStackElements(@Nonnull int[] itemIndices, @Nonnegative int nItems) {
      for (int i = 0; nItems > 0; i++, nItems--) {
         int itemType = itemIndices[i];
         writeFrameDefinition(itemType);

         if (itemType == FrameTypeMask.LONG || itemType == FrameTypeMask.DOUBLE) {
            i++;
         }
      }
   }

   public void emitFrameForUnreachableBlock(@Nonnegative int startOffset) {
      startFrame(startOffset, 0, 1);
      int frameValue = FrameTypeMask.OBJECT | cp.addNormalType("java/lang/Throwable");
      writeFrameDefinition(frameValue);
      endFrame();
   }

   @Nonnegative @Override
   public int getSize() { return stackMap == null ? 0 : 8 + stackMap.getLength(); }

   @Override
   public void put(@Nonnull ByteVector out) {
      if (stackMap != null) {
         put(out, 2 + stackMap.getLength());
         out.putShort(frameCount);
         out.putByteVector(stackMap);
      }
   }
}

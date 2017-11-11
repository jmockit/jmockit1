package mockit.external.asm;

import mockit.external.asm.Frame.*;

final class FrameAndStackComputation
{
   interface FrameType
   {
      /**
       * An expanded frame.
       */
      int NEW = -1;

      /**
       * A compressed frame with complete frame data.
       */
      int FULL = 0;

      /**
       * A compressed frame where locals are the same as the locals in the previous frame, except that additional 1-3
       * locals are defined, and with an empty stack.
       */
      int APPEND = 1;

      /**
       * A compressed frame where locals are the same as the locals in the previous frame, except that the last 1-3
       * locals are absent and with an empty stack.
       */
      int CHOP = 2;

      /**
       * A compressed frame with exactly the same locals as the previous frame and with an empty stack.
       */
      int SAME = 3;

      /**
       * A compressed frame with exactly the same locals as the previous frame and with a single value on the stack.
       */
      int SAME1 = 4;
   }

   /**
    * Constants that identify how many locals and stack items a frame has, with respect to its previous frame.
    */
   interface LocalsAndStackItemsDiff
   {
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

   private final MethodWriter mw;
   private final ClassWriter cw;

   /**
    * Maximum stack size of this method.
    */
   private int maxStack;

   /**
    * Maximum number of local variables for this method.
    */
   private int maxLocals;

   /**
    * Number of local variables in the current stack map frame.
    */
   private int currentLocals;

   /**
    * Number of stack map frames in the StackMapTable attribute.
    */
   private int frameCount;

   /**
    * The StackMapTable attribute.
    */
   private ByteVector stackMap;

   /**
    * The offset of the last frame that was written in the StackMapTable attribute.
    */
   private int previousFrameOffset;

   /**
    * The last frame that was written in the StackMapTable attribute.
    *
    * @see #frameDefinition
    */
   private int[] previousFrame;

   /**
    * The current stack map frame.
    * <p/>
    * The first element contains the offset of the instruction to which the frame corresponds (frameDefinition[0] =
    * offset), the second element is the number of locals (frameDefinition[1] = nLocal) and the third one is the number
    * of stack elements (frameDefinition[2] = nStack).
    * The local variables start at index 3 (frameDefinition[3 to 3+nLocal-1]) and are followed by the operand stack
    * values (frameDefinition[3+nLocal...]).
    * <p/>
    * All types are encoded as integers, with the same format as the one used in {@link Label}, but limited to BASE
    * types.
    */
   private int[] frameDefinition;

   /**
    * The current index in {@link #frameDefinition}, when writing new values into the array.
    */
   private int frameIndex;

   FrameAndStackComputation(MethodWriter mw, int methodAccess, String methodDesc) {
      this.mw = mw;
      cw = mw.cw;

      int size = Type.getArgumentsAndReturnSizes(methodDesc) >> 2;

      if ((methodAccess & Access.STATIC) != 0) {
         size--;
      }

      maxLocals = size;
      currentLocals = size;
   }

   void setMaxStack(int maxStack) {
      this.maxStack = maxStack;
   }

   void updateMaxLocals(int n) {
      if (n > maxLocals) {
         maxLocals = n;
      }
   }

   void putMaxStackAndLocals(ByteVector out) {
      out.putShort(maxStack).putShort(maxLocals);
   }

   void readFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
      if (type == FrameType.NEW) {
         readExpandedFrame(nLocal, local, nStack, stack);
      }
      else {
         int delta = getDeltaForType(type);

         if (delta < 0) {
            return;
         }

         switch (type) {
            case FrameType.FULL:
               readFullCompressedFrame(nLocal, local, nStack, stack, delta);
               break;
            case FrameType.APPEND:
               readCompressedFrame(nLocal, local, delta);
               break;
            case FrameType.CHOP:
               readCompressedFrameWithChoppedLocals(nLocal, delta);
               break;
            case FrameType.SAME:
               readCompressedFrameWithEmptyStack(delta);
               break;
            case FrameType.SAME1:
               readCompressedWithSingleValueOnStack(stack[0], delta);
               break;
         }

         previousFrameOffset = mw.code.length;
         frameCount++;
      }

      maxStack = Math.max(maxStack, nStack);
      maxLocals = Math.max(maxLocals, currentLocals);
   }

   private int getDeltaForType(int type) {
      int codeLength = mw.code.length;

      if (stackMap == null) {
         stackMap = new ByteVector();
         return codeLength;
      }

      int delta = codeLength - previousFrameOffset - 1;

      if (delta < 0) {
         if (type == FrameType.SAME) {
            return delta;
         }

         throw new IllegalStateException("Unexpected frame type: " + type);
      }

      return delta;
   }

   private void readFullCompressedFrame(int nLocal, Object[] local, int nStack, Object[] stack, int delta) {
      currentLocals = nLocal;
      stackMap.putByte(LocalsAndStackItemsDiff.FULL_FRAME).putShort(delta).putShort(nLocal);

      for (int i = 0; i < nLocal; ++i) {
         writeFrameType(local[i]);
      }

      stackMap.putShort(nStack);

      for (int i = 0; i < nStack; ++i) {
         writeFrameType(stack[i]);
      }
   }

   private void readCompressedFrame(int nLocal, Object[] local, int delta) {
      currentLocals += nLocal;
      stackMap.putByte(LocalsAndStackItemsDiff.SAME_FRAME_EXTENDED + nLocal).putShort(delta);

      for (int i = 0; i < nLocal; ++i) {
         writeFrameType(local[i]);
      }
   }

   private void readCompressedFrameWithChoppedLocals(int nLocal, int delta) {
      currentLocals -= nLocal;
      stackMap.putByte(LocalsAndStackItemsDiff.SAME_FRAME_EXTENDED - nLocal).putShort(delta);
   }

   private void readCompressedFrameWithEmptyStack(int delta) {
      if (delta < 64) {
         stackMap.putByte(delta);
      }
      else {
         stackMap.putByte(LocalsAndStackItemsDiff.SAME_FRAME_EXTENDED).putShort(delta);
      }
   }

   private void readCompressedWithSingleValueOnStack(Object type, int delta) {
      if (delta < 64) {
         stackMap.putByte(LocalsAndStackItemsDiff.SAME_LOCALS_1_STACK_ITEM_FRAME + delta);
      }
      else {
         stackMap.putByte(LocalsAndStackItemsDiff.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED).putShort(delta);
      }

      writeFrameType(type);
   }

   private void readExpandedFrame(int nLocal, Object[] local, int nStack, Object[] stack) {
      if (previousFrame == null) {
         visitImplicitFirstFrame();
      }

      currentLocals = nLocal;
      startFrame(mw.code.length, nLocal, nStack);

      for (int i = 0; i < nLocal; ++i) {
         Object localType = local[i];
         int frame;

         if (localType instanceof String) {
            frame = TypeMask.OBJECT | cw.addType((String) localType);
         }
         else if (localType instanceof Integer) {
            frame = (Integer) localType;
         }
         else {
            frame = TypeMask.UNINITIALIZED | cw.addUninitializedType("", ((Label) localType).position);
         }

         writeFrameDefinition(frame);
      }

      for (int i = 0; i < nStack; ++i) {
         Object stackType = stack[i];
         int frame;

         if (stackType instanceof String) {
            frame = TypeMask.OBJECT | cw.addType((String) stackType);
         }
         else if (stackType instanceof Integer) {
            frame = (Integer) stackType;
         }
         else {
            frame = TypeMask.UNINITIALIZED | cw.addUninitializedType("", ((Label) stackType).position);
         }

         writeFrameDefinition(frame);
      }

      endFrame();
   }

   private void visitImplicitFirstFrame() {
      int access = mw.access;
      String desc = mw.descriptor;

      // There can be at most descriptor.length() + 1 locals.
      startFrame(0, desc.length() + 1, 0);

      if ((access & Access.STATIC) == 0) {
         int frame = Access.isConstructor(access) ? 6 /* Frame.UNINITIALIZED_THIS */ :
            TypeMask.OBJECT | cw.addType(cw.thisName);

         writeFrameDefinition(frame);
      }

      int i = 1;

   loop:
      while (true) {
         int j = i;
         char typeCode = desc.charAt(i++);

         switch (typeCode) {
            case 'Z':
            case 'C':
            case 'B':
            case 'S':
            case 'I':
               writeFrameDefinition(1); // INTEGER
               break;
            case 'F':
               writeFrameDefinition(2); // FLOAT
               break;
            case 'J':
               writeFrameDefinition(4); // LONG
               break;
            case 'D':
               writeFrameDefinition(3); // DOUBLE
               break;
            case '[':
               i = writeArrayType(desc, i, j);
               break;
            case 'L':
               i = writeReferenceType(desc, i, j);
               break;
            default:
               break loop;
         }
      }

      setNumLocals(frameIndex - 3);
      endFrame();
   }

   private int getInstructionOffset() { return frameDefinition[0]; }
   private void setInstructionOffset(int offset) { frameDefinition[0] = offset; }

   private int getNumLocals() { return frameDefinition[1]; }
   private void setNumLocals(int numLocals) { frameDefinition[1] = numLocals; }

   private int getStackSize() { return frameDefinition[2]; }
   private void setStackSize(int stackSize) { frameDefinition[2] = stackSize; }

   private void writeFrameDefinition(int value) { frameDefinition[frameIndex++] = value; }

   private int writeArrayType(String desc, int i, int j) {
      while (desc.charAt(i) == '[') {
         i++;
      }

      if (desc.charAt(i) == 'L') {
         i++;

         while (desc.charAt(i) != ';') {
            i++;
         }
      }

      int frameValue = TypeMask.OBJECT | cw.addType(desc.substring(j, ++i));
      writeFrameDefinition(frameValue);
      return i;
   }

   private int writeReferenceType(String desc, int i, int j) {
      while (desc.charAt(i) != ';') {
         i++;
      }

      int frameValue = TypeMask.OBJECT | cw.addType(desc.substring(j + 1, i++));
      writeFrameDefinition(frameValue);
      return i;
   }

   private void writeFrameType(Object type) {
      if (type instanceof String) {
         stackMap.putByte(7).putShort(cw.newClass((String) type));
      }
      else if (type instanceof Integer) {
         stackMap.putByte((Integer) type);
      }
      else {
         stackMap.putByte(8).putShort(((Label) type).position);
      }
   }

   boolean hasStackMap() { return stackMap != null; }

   /**
    * Starts the visit of a stack map frame.
    * Sets {@link #frameIndex} to the index of the next element to be written in this frame.
    *
    * @param offset the offset of the instruction to which the frame corresponds.
    * @param nLocals the number of local variables in the frame.
    * @param nStack the number of stack elements in the frame.
    */
   private void startFrame(int offset, int nLocals, int nStack) {
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
    * Checks if the visit of the current {@link #frameDefinition frame} is finished, and if yes, write it in the
    * StackMapTable attribute.
    */
   private void endFrame() {
      if (previousFrame != null) { // do not write the first frame
         if (stackMap == null) {
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
      int currentFrameLocalsSize = getNumLocals();
      int currentFrameStackSize = getStackSize();

      if (cw.getClassVersion() < ClassVersion.V1_6) {
         writeFrameForOldVersionOfJava(currentFrameLocalsSize, currentFrameStackSize);
         return;
      }

      int type = LocalsAndStackItemsDiff.FULL_FRAME;
      int localsSize = previousFrame[1];
      int k = currentFrameStackSize == 0 ? currentFrameLocalsSize - localsSize : 0;
      int delta = getDelta();

      if (currentFrameStackSize == 0) {
         switch (k) {
            case -3:
            case -2:
            case -1:
               type = LocalsAndStackItemsDiff.CHOP_FRAME;
               localsSize = currentFrameLocalsSize;
               break;
            case 0:
               type = delta < 64 ? LocalsAndStackItemsDiff.SAME_FRAME : LocalsAndStackItemsDiff.SAME_FRAME_EXTENDED;
               break;
            case 1:
            case 2:
            case 3:
               type = LocalsAndStackItemsDiff.APPEND_FRAME;
               break;
         }
      }
      else if (currentFrameLocalsSize == localsSize && currentFrameStackSize == 1) {
         type = delta < 63 ?
            LocalsAndStackItemsDiff.SAME_LOCALS_1_STACK_ITEM_FRAME :
            LocalsAndStackItemsDiff.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED;
      }

      type = chooseTypeAsFullFrameIfApplicable(localsSize, type);

      switch (type) {
         case LocalsAndStackItemsDiff.SAME_FRAME:
            stackMap.putByte(delta);
            break;
         case LocalsAndStackItemsDiff.SAME_LOCALS_1_STACK_ITEM_FRAME:
            stackMap.putByte(LocalsAndStackItemsDiff.SAME_LOCALS_1_STACK_ITEM_FRAME + delta);
            writeFrameTypes(3 + currentFrameLocalsSize, 4 + currentFrameLocalsSize);
            break;
         case LocalsAndStackItemsDiff.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED:
            stackMap.putByte(LocalsAndStackItemsDiff.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED).putShort(delta);
            writeFrameTypes(3 + currentFrameLocalsSize, 4 + currentFrameLocalsSize);
            break;
         case LocalsAndStackItemsDiff.SAME_FRAME_EXTENDED:
            stackMap.putByte(LocalsAndStackItemsDiff.SAME_FRAME_EXTENDED).putShort(delta);
            break;
         case LocalsAndStackItemsDiff.CHOP_FRAME:
            stackMap.putByte(LocalsAndStackItemsDiff.SAME_FRAME_EXTENDED + k).putShort(delta);
            break;
         case LocalsAndStackItemsDiff.APPEND_FRAME:
            stackMap.putByte(LocalsAndStackItemsDiff.SAME_FRAME_EXTENDED + k).putShort(delta);
            writeFrameTypes(3 + localsSize, 3 + currentFrameLocalsSize);
            break;
         // case FULL_FRAME:
         default:
            stackMap.putByte(LocalsAndStackItemsDiff.FULL_FRAME).putShort(delta).putShort(currentFrameLocalsSize);
            writeFrameTypes(3, 3 + currentFrameLocalsSize);
            stackMap.putShort(currentFrameStackSize);
            writeFrameTypes(3 + currentFrameLocalsSize, 3 + currentFrameLocalsSize + currentFrameStackSize);
      }
   }

   private void writeFrameForOldVersionOfJava(int currentFrameLocalsSize, int currentFrameStackSize) {
      stackMap.putShort(getInstructionOffset()).putShort(currentFrameLocalsSize);
      writeFrameTypes(3, 3 + currentFrameLocalsSize);

      stackMap.putShort(currentFrameStackSize);
      writeFrameTypes(3 + currentFrameLocalsSize, 3 + currentFrameLocalsSize + currentFrameStackSize);
   }

   private int getDelta() {
      int offset = getInstructionOffset();
      return frameCount == 0 ? offset : offset - previousFrame[0] - 1;
   }

   private int chooseTypeAsFullFrameIfApplicable(int localsSize, int type) {
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
    * Writes some types of the current {@link #frameDefinition frame} into the StackMapTableAttribute. This method
    * converts types from the format used in {@link Label} to the format used in StackMapTable attributes. In
    * particular, it converts type table indexes to constant pool indexes.
    *
    * @param start index of the first type in {@link #frameDefinition} to write.
    * @param end   index of last type in {@link #frameDefinition} to write (exclusive).
    */
   private void writeFrameTypes(int start, int end) {
      for (int i = start; i < end; i++) {
         int type = frameDefinition[i];
         int dimensions = type & TypeMask.DIM;

         if (dimensions == 0) {
            writeFrameOfRegularType(type);
         }
         else {
            writeFrameOfArrayType(dimensions, type);
         }
      }
   }

   private void writeFrameOfRegularType(int type) {
      int v = type & TypeMask.BASE_VALUE;

      switch (type & TypeMask.BASE_KIND) {
         case TypeMask.OBJECT:
            String classDesc = cw.typeTable[v].strVal1;
            stackMap.putByte(7).putShort(cw.newClass(classDesc));
            break;
         case TypeMask.UNINITIALIZED:
            int typeDesc = cw.typeTable[v].intVal;
            stackMap.putByte(8).putShort(typeDesc);
            break;
         default:
            stackMap.putByte(v);
      }
   }

   private void writeFrameOfArrayType(int arrayDimensions, int arrayElementType) {
      StringBuilder sb = new StringBuilder();
      arrayDimensions >>= 28;

      while (arrayDimensions-- > 0) {
         sb.append('[');
      }

      if ((arrayElementType & TypeMask.BASE_KIND) == TypeMask.OBJECT) {
         Item arrayElementTypeItem = cw.typeTable[arrayElementType & TypeMask.BASE_VALUE];
         sb.append('L').append(arrayElementTypeItem.strVal1).append(';');
      }
      else {
         switch (arrayElementType & 0xF) {
            case 1:
               sb.append('I');
               break;
            case 2:
               sb.append('F');
               break;
            case 3:
               sb.append('D');
               break;
            case 9:
               sb.append('Z');
               break;
            case 10:
               sb.append('B');
               break;
            case 11:
               sb.append('C');
               break;
            case 12:
               sb.append('S');
               break;
            default:
               sb.append('J');
         }
      }

      String arrayElementTypeDesc = sb.toString();
      stackMap.putByte(7).putShort(cw.newClass(arrayElementTypeDesc));
   }

   // Creates and visits the first (implicit) frame.
   void createAndVisitFirstFrame(Frame frame) {
      Type[] args = Type.getArgumentTypes(mw.descriptor);
      frame.initInputFrame(cw, mw.access, args, maxLocals);
      visitFrame(frame);
   }

   /**
    * Visits a frame that has been computed from scratch.
    */
   void visitFrame(Frame f) {
      int[] locals = f.inputLocals;
      int nLocal = computeNumberOfLocals(locals);

      int[] stacks = f.inputStack;
      int nStack = computeStackSize(stacks);

      startFrame(f.owner.position, nLocal, nStack);
      putLocalsOrStackElements(locals, nLocal);
      putLocalsOrStackElements(stacks, nStack);
      endFrame();
   }

   // Computes the number of locals (ignores TOP types that are just after a LONG or a DOUBLE, and all trailing TOP
   // types).
   private int computeNumberOfLocals(int[] locals) {
      int nLocal = 0;
      int nTop = 0;

      for (int i = 0; i < locals.length; i++) {
         int t = locals[i];

         if (t == TypeMask.TOP) {
            nTop++;
         }
         else {
            nLocal += nTop + 1;
            nTop = 0;
         }

         if (t == TypeMask.LONG || t == TypeMask.DOUBLE) {
            i++;
         }
      }

      return nLocal;
   }

   // Computes the stack size (ignores TOP types that are just after a LONG or a DOUBLE).
   private int computeStackSize(int[] stacks) {
      int nStack = 0;

      for (int i = 0; i < stacks.length; i++) {
         int t = stacks[i];
         nStack++;

         if (t == TypeMask.LONG || t == TypeMask.DOUBLE) {
            i++;
         }
      }

      return nStack;
   }

   private void putLocalsOrStackElements(int[] itemIndices, int nItems) {
      for (int i = 0; nItems > 0; i++, nItems--) {
         int itemType = itemIndices[i];
         writeFrameDefinition(itemType);

         if (itemType == TypeMask.LONG || itemType == TypeMask.DOUBLE) {
            i++;
         }
      }
   }

   void emitFrameForUnreachableBlock(int startOffset) {
      startFrame(startOffset, 0, 1);
      int frameValue = TypeMask.OBJECT | cw.addType("java/lang/Throwable");
      writeFrameDefinition(frameValue);
      endFrame();
   }

   int getSize() {
      return stackMap == null ? 0 : 8 + stackMap.length;
   }

   int getSizeWhileAddingConstantPoolItem() {
      int size = getSize();

      if (size > 0) {
         boolean zip = cw.getClassVersion() >= ClassVersion.V1_6;
         cw.newUTF8(zip ? "StackMapTable" : "StackMap");
      }

      return size;
   }

   void put(ByteVector out) {
      if (stackMap != null) {
         boolean zip = cw.getClassVersion() >= ClassVersion.V1_6;
         out.putShort(cw.newUTF8(zip ? "StackMapTable" : "StackMap"));
         out.putInt(stackMap.length + 2).putShort(frameCount);
         out.putByteVector(stackMap);
      }
   }
}

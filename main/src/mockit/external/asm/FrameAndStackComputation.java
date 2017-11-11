package mockit.external.asm;

import static mockit.external.asm.Opcodes.*;

final class FrameAndStackComputation
{
   /**
    * Frame has exactly the same locals as the previous stack map frame and number of stack items is zero.
    */
   private static final int SAME_FRAME = 0; // to 63 (0-3f)

   /**
    * Frame has exactly the same locals as the previous stack map frame and number of stack items is 1.
    */
   private static final int SAME_LOCALS_1_STACK_ITEM_FRAME = 64; // to 127 (40-7f)

   /**
    * Frame has exactly the same locals as the previous stack map frame and number of stack items is 1.
    * Offset is bigger then 63.
    */
   private static final int SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED = 247; // f7

   /**
    * Frame where current locals are the same as the locals in the previous frame, except that the k last locals are
    * absent. The value of k is given by the formula 251-frame_type.
    */
   private static final int CHOP_FRAME = 248; // to 250 (f8-fA)

   /**
    * Frame has exactly the same locals as the previous stack map frame and number of stack items is zero.
    * Offset is bigger then 63.
    */
   private static final int SAME_FRAME_EXTENDED = 251; // fb

   /**
    * Frame where current locals are the same as the locals in the previous frame, except that k additional locals are
    * defined. The value of k is given by the formula frame_type-251.
    */
   private static final int APPEND_FRAME = 252; // to 254 // fc-fe

   /**
    * Full frame.
    */
   private static final int FULL_FRAME = 255; // ff

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
    * The current stack map frame. The first element contains the offset of the instruction to which the frame
    * corresponds, the second element is the number of locals and the third one is the number of stack elements. The
    * local variables start at index 3 and are followed by the operand stack values. In summary, frameDefinition[0] =
    * offset, frameDefinition[1] = nLocal, frameDefinition[2] = nStack, frameDefinition[3] = nLocal. All types are
    * encoded as integers, with the same format as the one used in {@link Label}, but limited to BASE types.
    */
   private int[] frameDefinition;

   FrameAndStackComputation(MethodWriter mw, int methodAccess, String methodDesc) {
      this.mw = mw;
      cw = mw.cw;

      int size = Type.getArgumentsAndReturnSizes(methodDesc) >> 2;

      if ((methodAccess & Access.STATIC) != 0) {
         --size;
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
      if (type == F_NEW) {
         if (previousFrame == null) {
            visitImplicitFirstFrame();
         }

         currentLocals = nLocal;
         int frameIndex = startFrame(mw.code.length, nLocal, nStack);

         for (int i = 0; i < nLocal; ++i) {
            if (local[i] instanceof String) {
               frameDefinition[frameIndex++] = Frame.OBJECT | cw.addType((String) local[i]);
            }
            else if (local[i] instanceof Integer) {
               frameDefinition[frameIndex++] = (Integer) local[i];
            }
            else {
               frameDefinition[frameIndex++] = Frame.UNINITIALIZED | cw.addUninitializedType("", ((Label) local[i]).position);
            }
         }

         for (int i = 0; i < nStack; ++i) {
            if (stack[i] instanceof String) {
               frameDefinition[frameIndex++] = Frame.OBJECT | cw.addType((String) stack[i]);
            }
            else if (stack[i] instanceof Integer) {
               frameDefinition[frameIndex++] = (Integer) stack[i];
            }
            else {
               frameDefinition[frameIndex++] = Frame.UNINITIALIZED | cw.addUninitializedType("", ((Label) stack[i]).position);
            }
         }

         endFrame();
      }
      else {
         int codeLength = mw.code.length;
         int delta;

         if (stackMap == null) {
            stackMap = new ByteVector();
            delta = codeLength;
         }
         else {
            delta = codeLength - previousFrameOffset - 1;

            if (delta < 0) {
               if (type == F_SAME) {
                  return;
               }

               throw new IllegalStateException();
            }
         }

         switch (type) {
            case F_FULL:
               currentLocals = nLocal;
               stackMap.putByte(FULL_FRAME).putShort(delta).putShort(nLocal);

               for (int i = 0; i < nLocal; ++i) {
                  writeFrameType(local[i]);
               }

               stackMap.putShort(nStack);

               for (int i = 0; i < nStack; ++i) {
                  writeFrameType(stack[i]);
               }

               break;
            case F_APPEND:
               currentLocals += nLocal;
               stackMap.putByte(SAME_FRAME_EXTENDED + nLocal).putShort(delta);

               for (int i = 0; i < nLocal; ++i) {
                  writeFrameType(local[i]);
               }

               break;
            case F_CHOP:
               currentLocals -= nLocal;
               stackMap.putByte(SAME_FRAME_EXTENDED - nLocal).putShort(delta);
               break;
            case F_SAME:
               if (delta < 64) {
                  stackMap.putByte(delta);
               }
               else {
                  stackMap.putByte(SAME_FRAME_EXTENDED).putShort(delta);
               }

               break;
            case F_SAME1:
               if (delta < 64) {
                  stackMap.putByte(SAME_LOCALS_1_STACK_ITEM_FRAME + delta);
               }
               else {
                  stackMap.putByte(SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED).putShort(delta);
               }

               writeFrameType(stack[0]);
               break;
         }

         previousFrameOffset = mw.code.length;
         ++frameCount;
      }

      maxStack = Math.max(maxStack, nStack);
      maxLocals = Math.max(maxLocals, currentLocals);
   }

   private void visitImplicitFirstFrame() {
      int access = mw.access;
      String desc = mw.descriptor;

      // There can be at most descriptor.length() + 1 locals.
      int frameIndex = startFrame(0, desc.length() + 1, 0);

      if ((access & Access.STATIC) == 0) {
         if ((access & Access.CONSTRUCTOR) == 0) {
            frameDefinition[frameIndex++] = Frame.OBJECT | cw.addType(cw.thisName);
         }
         else {
            frameDefinition[frameIndex++] = 6; // Opcodes.UNINITIALIZED_THIS;
         }
      }

      int i = 1;

      loop:
      while (true) {
         int j = i;
         switch (desc.charAt(i++)) {
            case 'Z':
            case 'C':
            case 'B':
            case 'S':
            case 'I':
               frameDefinition[frameIndex++] = 1; // Opcodes.INTEGER;
               break;
            case 'F':
               frameDefinition[frameIndex++] = 2; // Opcodes.FLOAT;
               break;
            case 'J':
               frameDefinition[frameIndex++] = 4; // Opcodes.LONG;
               break;
            case 'D':
               frameDefinition[frameIndex++] = 3; // Opcodes.DOUBLE;
               break;
            case '[':
               while (desc.charAt(i) == '[') {
                  ++i;
               }

               if (desc.charAt(i) == 'L') {
                  ++i;

                  while (desc.charAt(i) != ';') {
                     ++i;
                  }
               }

               frameDefinition[frameIndex++] = Frame.OBJECT | cw.addType(desc.substring(j, ++i));
               break;
            case 'L':
               while (desc.charAt(i) != ';') {
                  ++i;
               }

               frameDefinition[frameIndex++] = Frame.OBJECT | cw.addType(desc.substring(j + 1, i++));
               break;
            default:
               break loop;
         }
      }

      frameDefinition[1] = frameIndex - 3;
      endFrame();
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
    *
    * @param offset the offset of the instruction to which the frame corresponds.
    * @param nLocal the number of local variables in the frame.
    * @param nStack the number of stack elements in the frame.
    * @return the index of the next element to be written in this frame.
    */
   private int startFrame(int offset, int nLocal, int nStack) {
      int n = 3 + nLocal + nStack;

      if (frameDefinition == null || frameDefinition.length < n) {
         frameDefinition = new int[n];
      }

      frameDefinition[0] = offset;
      frameDefinition[1] = nLocal;
      frameDefinition[2] = nStack;
      return 3;
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
         ++frameCount;
      }

      previousFrame = frameDefinition;
      frameDefinition = null;
   }

   /**
    * Compress and writes the current {@link #frameDefinition frame} in the StackMapTable attribute.
    */
   private void writeFrame() {
      int currentFrameLocalsSize = frameDefinition[1];
      int currentFrameStackSize = frameDefinition[2];

      if (cw.getClassVersion() < V1_6) {
         stackMap.putShort(frameDefinition[0]).putShort(currentFrameLocalsSize);
         writeFrameTypes(3, 3 + currentFrameLocalsSize);

         stackMap.putShort(currentFrameStackSize);
         writeFrameTypes(3 + currentFrameLocalsSize, 3 + currentFrameLocalsSize + currentFrameStackSize);
         return;
      }

      int localsSize = previousFrame[1];
      int type = FULL_FRAME;
      int k = 0;
      int delta;

      if (frameCount == 0) {
         delta = frameDefinition[0];
      }
      else {
         delta = frameDefinition[0] - previousFrame[0] - 1;
      }

      if (currentFrameStackSize == 0) {
         k = currentFrameLocalsSize - localsSize;

         switch (k) {
            case -3:
            case -2:
            case -1:
               type = CHOP_FRAME;
               localsSize = currentFrameLocalsSize;
               break;
            case 0:
               type = delta < 64 ? SAME_FRAME : SAME_FRAME_EXTENDED;
               break;
            case 1:
            case 2:
            case 3:
               type = APPEND_FRAME;
               break;
         }
      }
      else if (currentFrameLocalsSize == localsSize && currentFrameStackSize == 1) {
         type = delta < 63 ? SAME_LOCALS_1_STACK_ITEM_FRAME : SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED;
      }

      if (type != FULL_FRAME) {
         // Verify if locals are the same.
         int l = 3;

         for (int j = 0; j < localsSize; j++) {
            if (frameDefinition[l] != previousFrame[l]) {
               type = FULL_FRAME;
               break;
            }

            l++;
         }
      }

      switch (type) {
         case SAME_FRAME:
            stackMap.putByte(delta);
            break;
         case SAME_LOCALS_1_STACK_ITEM_FRAME:
            stackMap.putByte(SAME_LOCALS_1_STACK_ITEM_FRAME + delta);
            writeFrameTypes(3 + currentFrameLocalsSize, 4 + currentFrameLocalsSize);
            break;
         case SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED:
            stackMap.putByte(SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED).putShort(delta);
            writeFrameTypes(3 + currentFrameLocalsSize, 4 + currentFrameLocalsSize);
            break;
         case SAME_FRAME_EXTENDED:
            stackMap.putByte(SAME_FRAME_EXTENDED).putShort(delta);
            break;
         case CHOP_FRAME:
            stackMap.putByte(SAME_FRAME_EXTENDED + k).putShort(delta);
            break;
         case APPEND_FRAME:
            stackMap.putByte(SAME_FRAME_EXTENDED + k).putShort(delta);
            writeFrameTypes(3 + localsSize, 3 + currentFrameLocalsSize);
            break;
         // case FULL_FRAME:
         default:
            stackMap.putByte(FULL_FRAME).putShort(delta).putShort(currentFrameLocalsSize);
            writeFrameTypes(3, 3 + currentFrameLocalsSize);
            stackMap.putShort(currentFrameStackSize);
            writeFrameTypes(3 + currentFrameLocalsSize, 3 + currentFrameLocalsSize + currentFrameStackSize);
      }
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
      for (int i = start; i < end; ++i) {
         int t = frameDefinition[i];
         int d = t & Frame.DIM;

         if (d == 0) {
            int v = t & Frame.BASE_VALUE;

            switch (t & Frame.BASE_KIND) {
               case Frame.OBJECT:
                  stackMap.putByte(7).putShort(cw.newClass(cw.typeTable[v].strVal1));
                  break;
               case Frame.UNINITIALIZED:
                  stackMap.putByte(8).putShort(cw.typeTable[v].intVal);
                  break;
               default:
                  stackMap.putByte(v);
            }
         }
         else {
            StringBuilder sb = new StringBuilder();
            d >>= 28;

            while (d-- > 0) {
               sb.append('[');
            }

            if ((t & Frame.BASE_KIND) == Frame.OBJECT) {
               sb.append('L');
               sb.append(cw.typeTable[t & Frame.BASE_VALUE].strVal1);
               sb.append(';');
            }
            else {
               switch (t & 0xF) {
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

            stackMap.putByte(7).putShort(cw.newClass(sb.toString()));
         }
      }
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
      int i, t;
      int nTop = 0;
      int nLocal = 0;
      int nStack = 0;
      int[] locals = f.inputLocals;
      int[] stacks = f.inputStack;

      // Computes the number of locals (ignores TOP types that are just after a LONG or a DOUBLE, and all trailing TOP
      // types).
      for (i = 0; i < locals.length; ++i) {
         t = locals[i];

         if (t == Frame.TOP) {
            ++nTop;
         }
         else {
            nLocal += nTop + 1;
            nTop = 0;
         }

         if (t == Frame.LONG || t == Frame.DOUBLE) {
            ++i;
         }
      }

      // Computes the stack size (ignores TOP types that are just after a LONG or a DOUBLE).
      for (i = 0; i < stacks.length; ++i) {
         t = stacks[i];
         ++nStack;

         if (t == Frame.LONG || t == Frame.DOUBLE) {
            ++i;
         }
      }

      // Visits the frame and its content.
      int frameIndex = startFrame(f.owner.position, nLocal, nStack);

      for (i = 0; nLocal > 0; ++i, --nLocal) {
         t = locals[i];
         frameDefinition[frameIndex++] = t;

         if (t == Frame.LONG || t == Frame.DOUBLE) {
            ++i;
         }
      }

      for (i = 0; i < stacks.length; ++i) {
         t = stacks[i];
         frameDefinition[frameIndex++] = t;

         if (t == Frame.LONG || t == Frame.DOUBLE) {
            ++i;
         }
      }

      endFrame();
   }

   void emitFrameForUnreachableBlock(int startOffset) {
      int frameIndex = startFrame(startOffset, 0, 1);
      frameDefinition[frameIndex] = Frame.OBJECT | cw.addType("java/lang/Throwable");
      endFrame();
   }

   int getSize() {
      return stackMap == null ? 0 : 8 + stackMap.length;
   }

   int getSizeWhileAddingConstantPoolItem() {
      int size = getSize();

      if (size > 0) {
         boolean zip = cw.getClassVersion() >= V1_6;
         cw.newUTF8(zip ? "StackMapTable" : "StackMap");
      }

      return size;
   }

   void put(ByteVector out) {
      if (stackMap != null) {
         boolean zip = cw.getClassVersion() >= V1_6;
         out.putShort(cw.newUTF8(zip ? "StackMapTable" : "StackMap"));
         out.putInt(stackMap.length + 2).putShort(frameCount);
         out.putByteVector(stackMap);
      }
   }
}

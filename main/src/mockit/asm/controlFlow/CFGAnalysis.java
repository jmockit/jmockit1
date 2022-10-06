package mockit.asm.controlFlow;

import javax.annotation.*;

import mockit.asm.constantPool.*;
import mockit.asm.jvmConstants.*;
import mockit.asm.util.*;
import static mockit.asm.jvmConstants.Opcodes.*;

/**
 * The control flow graph analysis algorithm, used to compute the maximum stack size for a method or constructor.
 * <p/>
 * A control flow graph contains one node per "basic block", and one edge per "jump" from one basic block to another.
 * Each node (i.e., each basic block) is represented by the Label object that corresponds to the first instruction of this basic block.
 * Each node also stores the list of its successors in the graph, as a linked list of Edge objects.
 */
@SuppressWarnings("OverlyComplexClass")
public final class CFGAnalysis
{
   @Nonnull private final ConstantPoolGeneration cp;
   @Nonnull private final String classDesc;
   @Nonnull private final ByteVector code;

   /**
    * Indicates whether frames AND max stack/locals must be automatically computed, or if only max stack/locals must be.
    */
   private final boolean computeFrames;

   /**
    * A list of labels. This list is the list of basic blocks in the method, i.e. a list of Label objects linked to each other by their
    * {@link Label#successor} field, in the order they are visited, and starting with the first basic block.
    */
   @Nonnull private final Label labels;

   /**
    * The previous basic block.
    */
   @Nullable private Label previousBlock;

   /**
    * The current basic block.
    */
   @Nullable private Label currentBlock;

   /**
    * The (relative) stack size after the last visited instruction. This size is relative to the beginning of the current basic block, i.e.,
    * the true stack size after the last visited instruction is equal to the {@link Label#inputStackTop beginStackSize} of the current basic
    * block plus <tt>stackSize</tt>.
    */
   @Nonnegative private int stackSize;

   /**
    * The (relative) maximum stack size after the last visited instruction. This size is relative to the beginning of the current basic
    * block, i.e., the true maximum stack size after the last visited instruction is equal to the {@link Label#inputStackTop beginStackSize}
    * of the current basic block plus <tt>stackSize</tt>.
    */
   @Nonnegative private int maxStackSize;

   public CFGAnalysis(@Nonnull ConstantPoolGeneration cp, @Nonnull String classDesc, @Nonnull ByteVector code, boolean computeFrames) {
      this.cp = cp;
      this.classDesc = classDesc;
      this.code = code;
      this.computeFrames = computeFrames;

      labels = new Label();
      labels.markAsPushed();
      updateCurrentBlockForLabelBeforeNextInstruction(labels);
   }

   @Nonnull public Label getLabelForFirstBasicBlock() { return labels; }
   public Frame getFirstFrame() { return labels.frame; }
   @Nullable public Label getLabelForCurrentBasicBlock() { return currentBlock; }

   public void updateCurrentBlockForZeroOperandInstruction(int opcode) {
      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.execute(opcode);
         }
         else {
            int sizeVariation = JVMInstruction.SIZE[opcode];
            updateStackSize(sizeVariation);
         }

         // If opcode == ATHROW or xRETURN, ends current block (no successor).
         if ((opcode >= IRETURN && opcode <= RETURN) || opcode == ATHROW) {
            noSuccessor();
         }
      }
   }

   // Updates current and max stack sizes.
   private void updateStackSize(int sizeVariation) {
      int newSize = stackSize + sizeVariation;

      if (newSize > maxStackSize) {
         maxStackSize = newSize;
      }

      stackSize = newSize;
   }

   /**
    * Ends the current basic block. This method must be used in the case where the current basic block does not have any successor.
    */
   private void noSuccessor() {
      if (computeFrames) {
         Label l = new Label();
         l.frame = new Frame(cp, l);
         l.resolve(code);
         //noinspection ConstantConditions
         previousBlock.successor = l;
         previousBlock = l;
      }
      else {
         //noinspection ConstantConditions
         currentBlock.outputStackMax = maxStackSize;
      }

      currentBlock = null;
   }

   /**
    * Adds a successor to the {@link #currentBlock currentBlock} block.
    *
    * @param info      information about the control flow edge to be added.
    * @param successor the successor block to be added to the current block.
    */
   private void addSuccessor(int info, @Nonnull Label successor) {
      // Creates and initializes an Edge object...
      Edge edge = new Edge(info, successor);

      // ...and adds it to the successor list of the current block.
      //noinspection ConstantConditions
      currentBlock.setSuccessors(edge);
   }

   public void updateCurrentBlockForSingleIntOperandInstruction(int opcode, int operand) {
      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.executeINT(opcode, operand);
         }
         else if (opcode != NEWARRAY) { // updates stack size only for NEWARRAY (variation = 0 for BIPUSH or SIPUSH)
            updateStackSize(1);
         }
      }
   }

   public void updateCurrentBlockForLocalVariableInstruction(int opcode, @Nonnegative int varIndex) {
      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.executeVAR(opcode, varIndex);
         }
         else { // xLOAD or xSTORE
            int sizeVariation = JVMInstruction.SIZE[opcode];
            updateStackSize(sizeVariation);
         }
      }
   }

   public void updateCurrentBlockForTypeInstruction(int opcode, @Nonnull StringItem typeItem) {
      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.executeTYPE(opcode, code.getLength(), typeItem);
         }
         else if (opcode == NEW) { // updates stack size for NEW only; no change for ANEWARRAY, CHECKCAST, INSTANCEOF
            updateStackSize(1);
         }
      }
   }

   public void updateCurrentBlockForFieldInstruction(int opcode, @Nonnull TypeOrMemberItem fieldItem, @Nonnull String fieldTypeDesc) {
      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.execute(opcode, fieldItem);
         }
         else {
            char typeCode = fieldTypeDesc.charAt(0);
            int sizeVariation = computeSizeVariationForFieldAccess(opcode, typeCode);
            updateStackSize(sizeVariation);
         }
      }
   }

   private static int computeSizeVariationForFieldAccess(int fieldAccessOpcode, char fieldTypeCode) {
      boolean doubleSizeType = fieldTypeCode == 'D' || fieldTypeCode == 'J';

      switch (fieldAccessOpcode) {
         case GETSTATIC: return doubleSizeType ? 2 : 1;
         case PUTSTATIC: return doubleSizeType ? -2 : -1;
         case GETFIELD:  return doubleSizeType ? 1 : 0;
         case PUTFIELD:  return doubleSizeType ? -3 : -2;
         default: throw new IllegalArgumentException("Unknown field access opcode: " + fieldAccessOpcode);
      }
   }

   public void updateCurrentBlockForInvokeInstruction(@Nonnull TypeOrMemberItem invokeItem, int opcode, @Nonnull String desc) {
      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.execute(opcode, invokeItem);
         }
         else {
            int argSize = invokeItem.getArgSizeComputingIfNeeded(desc);
            int sizeVariation = -(argSize >> 2) + (argSize & 0x03);

            if (opcode == INVOKESTATIC || opcode == INVOKEDYNAMIC) {
               sizeVariation++;
            }

            updateStackSize(sizeVariation);
         }
      }
   }

   @Nullable
   public Label updateCurrentBlockForJumpInstruction(int opcode, @Nonnull Label label) {
      Label nextInsn = null;

      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.executeJUMP(opcode);

            // 'label' is the target of a jump instruction.
            label.getFirst().markAsTarget();

            // Adds 'label' as a successor of this basic block.
            addSuccessor(Edge.NORMAL, label);

            if (opcode != GOTO) {
               // Creates a Label for the next basic block.
               nextInsn = new Label();
            }
         }
         else {
            // Updates current stack size (max size unchanged because size variation always negative in this case).
            stackSize += JVMInstruction.SIZE[opcode];
            addSuccessor(stackSize, label);
         }
      }

      return nextInsn;
   }

   public void updateCurrentBlockForJumpTarget(int opcode, @Nullable Label nextInsn) {
      if (currentBlock != null) {
         if (nextInsn != null) {
            // If the jump instruction is not a GOTO, the next instruction is also a successor of this instruction.
            // Calling visitLabel adds the label of this next instruction as a successor of the current block, and starts a new basic block.
            updateCurrentBlockForLabelBeforeNextInstruction(nextInsn);
         }

         if (opcode == GOTO) {
            noSuccessor();
         }
      }
   }

   public void updateCurrentBlockForLabelBeforeNextInstruction(@Nonnull Label label) {
      // Resolves previous forward references to label, if any.
      label.resolve(code);

      if (label.isDebug()) {
         return;
      }

      if (computeFrames) {
         if (currentBlock != null) {
            if (label.position == currentBlock.position) {
               // Successive labels, do not start a new basic block.
               currentBlock.markAsTarget(label);
               label.frame = currentBlock.frame;
               return;
            }

            // Ends current block (with one new successor).
            addSuccessor(Edge.NORMAL, label);
         }

         // Begins a new current block.
         currentBlock = label;

         if (label.frame == null) {
            label.frame = new Frame(cp, label);
         }

         // Updates the basic block list.
         if (previousBlock != null) {
            if (label.position == previousBlock.position) {
               previousBlock.markAsTarget(label);
               label.frame = previousBlock.frame;
               currentBlock = previousBlock;
               return;
            }

            previousBlock.successor = label;
         }
      }
      else {
         if (currentBlock != null) {
            // Ends current block (with one new successor).
            currentBlock.outputStackMax = maxStackSize;
            addSuccessor(stackSize, label);
         }

         // Begins a new current block
         currentBlock = label;

         // Resets the relative current and max stack sizes.
         stackSize = 0;
         maxStackSize = 0;

         // Updates the basic block list.
         if (previousBlock != null) {
            previousBlock.successor = label;
         }
      }

      previousBlock = label;
   }

   public void updateCurrentBlockForLDCInstruction(@Nonnull Item constItem) {
      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.executeLDC(constItem);
         }
         else {
            int sizeVariation = constItem instanceof LongValueItem ? 2 : 1;
            updateStackSize(sizeVariation);
         }
      }
   }

   public void updateCurrentBlockForIINCInstruction(@Nonnegative int varIndex) {
      if (currentBlock != null && computeFrames) {
         currentBlock.frame.executeIINC(varIndex);
      }
   }

   public void updateCurrentBlockForSwitchInstruction(@Nonnull Label dflt, @Nonnull Label[] caseLabels) {
      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.executeSWITCH();

            // Adds current block successors.
            addSuccessor(Edge.NORMAL, dflt);
            dflt.getFirst().markAsTarget();

            for (Label label : caseLabels) {
               addSuccessor(Edge.NORMAL, label);
               label.getFirst().markAsTarget();
            }
         }
         else {
            // Updates current stack size (max stack size unchanged).
            stackSize--;

            // Adds current block successors.
            addSuccessor(stackSize, dflt);
            addSuccessorForEachCase(caseLabels);
         }

         // Ends current block.
         noSuccessor();
      }
   }

   private void addSuccessorForEachCase(@Nonnull Label[] caseLabels) {
      for (Label label : caseLabels) {
         addSuccessor(stackSize, label);
      }
   }

   public void updateCurrentBlockForMULTIANEWARRAYInstruction(@Nonnull StringItem arrayTypeItem, @Nonnegative int dims) {
      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.executeMULTIANEWARRAY(dims, arrayTypeItem);
         }
         else {
            // Updates current stack size (max stack size unchanged because stack size variation always negative or 0).
            stackSize += 1 - dims;
         }
      }
   }

   /**
    * Fix point algorithm: mark the first basic block as 'changed' (i.e. put it in the 'changed' list) and, while there are changed basic
    * blocks, choose one, mark it as unchanged, and update its successors (which can be changed in the process).
    */
   @Nonnegative
   public int computeMaxStackSizeFromComputedFrames() {
      int max = 0;
      Label changed = labels;

      while (changed != null) {
         // Removes a basic block from the list of changed basic blocks.
         Label l = changed;
         changed = changed.next;
         l.next = null;
         Frame frame = l.frame;

         // A reachable jump target must be stored in the stack map.
         if (l.isTarget()) {
            l.markAsStoringFrame();
         }

         // All visited labels are reachable, by definition.
         l.markAsReachable();

         // Updates the (absolute) maximum stack size.
         int blockMax = frame.inputStack.length + l.outputStackMax;

         if (blockMax > max) {
            max = blockMax;
         }

         changed = updateSuccessorsOfCurrentBasicBlock(changed, frame, l);
      }

      return max;
   }

   @Nullable
   private Label updateSuccessorsOfCurrentBasicBlock(@Nullable Label changed, @Nonnull Frame frame, @Nonnull Label label) {
      Edge edge = label.successors;

      while (edge != null) {
         Label n = edge.successor.getFirst();
         boolean change = frame.merge(classDesc, n.frame, edge.info);

         if (change && n.next == null) {
            // If n has changed and is not already in the 'changed' list, adds it to this list.
            n.next = changed;

            //noinspection AssignmentToMethodParameter
            changed = n;
         }

         edge = edge.next;
      }

      return changed;
   }

   /**
    * Control flow analysis algorithm: while the block stack is not empty, pop a block from this stack, update the max stack size, compute
    * the true (non relative) begin stack size of the successors of this block, and push these successors onto the stack (unless they have
    * already been pushed onto the stack). Note: by hypothesis, the {@link Label#inputStackTop} of the blocks in the block stack are the
    * true (non relative) beginning stack sizes of these blocks.
    */
   @Nonnegative
   public int computeMaxStackSize() {
      int max = 0;
      Label stack = labels;

      while (stack != null) {
         // Pops a block from the stack.
         Label label = stack;
         stack = stack.next;

         // Computes the true (non relative) max stack size of this block.
         int start = label.inputStackTop;
         int blockMax = start + label.outputStackMax;

         // Updates the global max stack size.
         if (blockMax > max) {
            max = blockMax;
         }

         stack = analyzeBlockSuccessors(stack, label, start);
      }

      return max;
   }

   @Nullable
   private static Label analyzeBlockSuccessors(@Nullable Label stack, @Nonnull Label label, @Nonnegative int start) {
      Edge block = label.successors;

      while (block != null) {
         Label successor = block.successor;

         // If this successor has not already been pushed...
         if (!successor.isPushed()) {
            // computes its true beginning stack size...
            successor.inputStackTop = block.info == Edge.EXCEPTION ? 1 : start + block.info;

            // ...and pushes it onto the stack.
            successor.markAsPushed();
            successor.next = stack;

            //noinspection AssignmentToMethodParameter
            stack = successor;
         }

         block = block.next;
      }

      return stack;
   }
}

package mockit.external.asm;

import static mockit.external.asm.Opcodes.*;

final class CFGAnalysis
{
   private final ClassWriter cw;
   private final ConstantPoolGeneration cp;
   private final ByteVector code;

   /**
    * The number of subroutines in this method.
    */
   private int subroutines;

   // Fields for the control flow graph analysis algorithm (used to compute the maximum stack size). A control flow
   // graph contains one node per "basic block", and one edge per "jump" from one basic block to another. Each node
   // (i.e., each basic block) is represented by the Label object that corresponds to the first instruction of this
   // basic block. Each node also stores the list of its successors in the graph, as a linked list of Edge objects.
   //

   /**
    * Indicates whether frames AND max stack/locals must be automatically computed, or if only max stack/locals must be.
    */
   private final boolean computeFrames;

   /**
    * A list of labels. This list is the list of basic blocks in the method, i.e. a list of Label objects linked to each
    * other by their {@link Label#successor} field, in the order they are visited by {@link MethodVisitor#visitLabel},
    * and starting with the first basic block.
    */
   private final Label labels;

   /**
    * The previous basic block.
    */
   private Label previousBlock;

   /**
    * The current basic block.
    */
   private Label currentBlock;

   /**
    * The (relative) stack size after the last visited instruction. This size is relative to the beginning of the
    * current basic block, i.e., the true stack size after the last visited instruction is equal to the
    * {@link Label#inputStackTop beginStackSize} of the current basic block plus <tt>stackSize</tt>.
    */
   private int stackSize;

   /**
    * The (relative) maximum stack size after the last visited instruction. This size is relative to the beginning of
    * the current basic block, i.e., the true maximum stack size after the last visited instruction is equal to the
    * {@link Label#inputStackTop beginStackSize} of the current basic block plus <tt>stackSize</tt>.
    */
   private int maxStackSize;

   CFGAnalysis(ClassWriter cw, ByteVector code, boolean computeFrames) {
      this.cw = cw;
      cp = cw.cp;
      this.code = code;
      this.computeFrames = computeFrames;

      labels = new Label();
      labels.markAsPushed();
      updateCurrentBlockForLabelBeforeNextInstruction(labels);
   }

   Label getLabelForFirstBasicBlock() { return labels; }
   Frame getFirstFrame() { return labels.frame; }
   Label getLabelForCurrentBasicBlock() { return currentBlock; }

   void updateCurrentBlockForZeroOperandInstruction(int opcode) {
      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.execute(opcode, 0, null, null);
         }
         else {
            int sizeVariation = Frame.SIZE[opcode];
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
    * Ends the current basic block. This method must be used in the case where the current basic block does not have any
    * successor.
    */
   private void noSuccessor() {
      if (computeFrames) {
         Label l = new Label();
         l.frame = new Frame(l);
         l.resolve(code);
         previousBlock.successor = l;
         previousBlock = l;
      }
      else {
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
   private void addSuccessor(int info, Label successor) {
      // Creates and initializes an Edge object...
      Edge b = new Edge(info, successor);

      // ...and adds it to the successor list of the currentBlock block.
      b.next = currentBlock.successors;
      currentBlock.successors = b;
   }

   void updateCurrentBlockForSingleIntOperandInstruction(int opcode, int operand) {
      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.execute(opcode, operand, null, null);
         }
         else if (opcode != NEWARRAY) { // updates stack size only for NEWARRAY (variation = 0 for BIPUSH or SIPUSH)
            updateStackSize(1);
         }
      }
   }

   void updateCurrentBlockForLocalVariableInstruction(int opcode, int var) {
      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.execute(opcode, var, null, null);
         }
         else {
            // Updates current and max stack sizes.
            if (opcode == RET) {
               // No stack change, but end of current block (no successor).
               currentBlock.markAsEndingWithRET();

               // Save 'stackSize' here for future use.
               currentBlock.inputStackTop = stackSize;
               noSuccessor();
            }
            else { // xLOAD or xSTORE
               int sizeVariation = Frame.SIZE[opcode];
               updateStackSize(sizeVariation);
            }
         }
      }
   }

   void updateCurrentBlockForTypeInstruction(int opcode, Item typeItem) {
      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.execute(opcode, code.length, cp, typeItem);
         }
         else if (opcode == NEW) { // updates stack size for NEW only; no change for ANEWARRAY, CHECKCAST, INSTANCEOF
            updateStackSize(1);
         }
      }
   }

   void updateCurrentBlockForFieldInstruction(int opcode, Item fieldItem, String desc) {
      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.execute(opcode, 0, cp, fieldItem);
         }
         else {
            char typeCode = desc.charAt(0);
            boolean doubleSizeType = typeCode == 'D' || typeCode == 'J';
            int sizeVariation;

            // Computes the stack size variation.
            switch (opcode) {
               case GETSTATIC:
                  sizeVariation = doubleSizeType ? 2 : 1;
                  break;
               case PUTSTATIC:
                  sizeVariation = doubleSizeType ? -2 : -1;
                  break;
               case GETFIELD:
                  sizeVariation = doubleSizeType ? 1 : 0;
                  break;
               // case PUTFIELD:
               default:
                  sizeVariation = doubleSizeType ? -3 : -2;
                  break;
            }

            updateStackSize(sizeVariation);
         }
      }
   }

   void updateCurrentBlockForInvokeInstruction(Item invokeItem, int opcode, String desc) {
      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.execute(opcode, 0, cp, invokeItem);
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

   Label updateCurrentBlockForJumpInstruction(int opcode, Label label) {
      Label nextInsn = null;

      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.execute(opcode, 0, null, null);

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
            if (opcode == JSR) {
               if (label.markAsSubroutine()) {
                  subroutines++;
               }

               currentBlock.markAsJSR();
               addSuccessor(stackSize + 1, label);

               // Creates a Label for the next basic block.
               nextInsn = new Label();

               // Note that, by construction in this method, a JSR block has at least two successors in the control flow
               // graph: the first one leads the next instruction after the JSR, while the second one leads to the JSR
               // target.
            }
            else {
               // Updates current stack size (max stack size unchanged because stack size variation always negative in
               // this case).
               stackSize += Frame.SIZE[opcode];
               addSuccessor(stackSize, label);
            }
         }
      }

      return nextInsn;
   }

   void updateCurrentBlockForJumpTarget(int opcode, Label nextInsn) {
      if (currentBlock != null) {
         if (nextInsn != null) {
            // If the jump instruction is not a GOTO, the next instruction is also a successor of this instruction.
            // Calling visitLabel adds the label of this next instruction as a successor of the current block, and
            // starts a new basic block.
            updateCurrentBlockForLabelBeforeNextInstruction(nextInsn);
         }

         if (opcode == GOTO) {
            noSuccessor();
         }
      }
   }

   void updateCurrentBlockForLabelBeforeNextInstruction(Label label) {
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
            label.frame = new Frame(label);
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

         previousBlock = label;
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

         previousBlock = label;
      }
   }

   void updateCurrentBlockForLDCInstruction(Item constItem) {
      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.execute(LDC, 0, cp, constItem);
         }
         else {
            updateStackSize(constItem.isDoubleSized() ? 2 : 1);
         }
      }
   }

   void updateCurrentBlockForIINCInstruction(int var) {
      if (currentBlock != null && computeFrames) {
         currentBlock.frame.execute(IINC, var, null, null);
      }
   }

   void updateCurrentBlockForSwitchInsn(Label dflt, Label[] labels) {
      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.execute(LOOKUPSWITCH, 0, null, null);

            // Adds current block successors.
            addSuccessor(Edge.NORMAL, dflt);
            dflt.getFirst().markAsTarget();

            for (int i = 0; i < labels.length; ++i) {
               addSuccessor(Edge.NORMAL, labels[i]);
               labels[i].getFirst().markAsTarget();
            }
         }
         else {
            // Updates current stack size (max stack size unchanged).
            stackSize--;

            // Adds current block successors.
            addSuccessor(stackSize, dflt);

            for (int i = 0; i < labels.length; ++i) {
               addSuccessor(stackSize, labels[i]);
            }
         }

         // Ends current block.
         noSuccessor();
      }
   }

   void updateCurrentBlockForMULTIANEWARRAYInstruction(Item arrayTypeItem, int dims) {
      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.execute(MULTIANEWARRAY, dims, cp, arrayTypeItem);
         }
         else {
            // Updates current stack size (max stack size unchanged because stack size variation always negative or 0).
            stackSize += 1 - dims;
         }
      }
   }

   /**
    * Fix point algorithm: mark the first basic block as 'changed' (i.e. put it in the 'changed' list) and, while there
    * are changed basic blocks, choose one, mark it as unchanged, and update its successors (which can be changed in the
    * process).
    */
   int computeMaxStackSizeFromComputedFrames() {
      int max = 0;
      Label changed = labels;
      Frame f;

      while (changed != null) {
         // Removes a basic block from the list of changed basic blocks.
         Label l = changed;
         changed = changed.next;
         l.next = null;
         f = l.frame;

         // A reachable jump target must be stored in the stack map.
         if (l.isTarget()) {
            l.markAsStoringFrame();
         }

         // All visited labels are reachable, by definition.
         l.markAsReachable();

         // Updates the (absolute) maximum stack size.
         int blockMax = f.inputStack.length + l.outputStackMax;

         if (blockMax > max) {
            max = blockMax;
         }

         // Updates the successors of the current basic block.
         Edge e = l.successors;

         while (e != null) {
            Label n = e.successor.getFirst();
            boolean change = f.merge(cw.thisName, cp, n.frame, e.info);

            if (change && n.next == null) {
               // If n has changed and is not already in the 'changed' list, adds it to this list.
               n.next = changed;
               changed = n;
            }

            e = e.next;
         }
      }

      return max;
   }

   /**
    * First step: finds the subroutines. This step determines, for each basic block, to which subroutine(s) it
    * belongs. Second step: finds the successors of RET blocks.
    */
   void completeControlFlowGraphWithRETSuccessors() {
      if (subroutines > 0) {
         // Finds the basic blocks that belong to the "main" subroutine.
         int id = 0;
         labels.visitSubroutine(null, 1, subroutines);

         // Finds the basic blocks that belong to the real subroutines.
         Label l = labels;

         while (l != null) {
            if (l.isJSR()) {
               // The subroutine is defined by l's TARGET, not by l.
               Label subroutine = l.successors.next.successor;

               // If this subroutine has not been visited yet...
               if (!subroutine.isVisited()) {
                  // ...assigns it a new id and finds its basic blocks.
                  id += 1;
                  subroutine.visitSubroutine(null, (id / 32L) << 32 | (1L << (id % 32)), subroutines);
               }
            }

            l = l.successor;
         }

         // Second step: finds the successors of RET blocks.
         l = labels;

         while (l != null) {
            if (l.isJSR()) {
               labels.markThisAndSuccessorsAsNotVisitedBySubroutine();

               // The subroutine is defined by l's TARGET, not by l.
               Label subroutine = l.successors.next.successor;
               subroutine.visitSubroutine(l, 0, subroutines);
            }

            l = l.successor;
         }
      }
   }

   /**
    * Control flow analysis algorithm: while the block stack is not empty, pop a block from this stack, update the max
    * stack size, compute the true (non relative) begin stack size of the successors of this block, and push these
    * successors onto the stack (unless they have already been pushed onto the stack).
    * Note: by hypothesis, the {@link Label#inputStackTop} of the blocks in the block stack are the true (non relative)
    * beginning stack sizes of these blocks.
    */
   int computeMaxStackSize() {
      int max = 0;
      Label stack = labels;

      while (stack != null) {
         // Pops a block from the stack.
         Label l = stack;
         stack = stack.next;

         // Computes the true (non relative) max stack size of this block.
         int start = l.inputStackTop;
         int blockMax = start + l.outputStackMax;

         // Updates the global max stack size.
         if (blockMax > max) {
            max = blockMax;
         }

         // Analyzes the successors of the block.
         Edge b = l.successors;

         if (l.isJSR()) {
            // Ignores the first edge of JSR blocks (virtual successor).
            b = b.next;
         }

         while (b != null) {
            l = b.successor;

            // If this successor has not already been pushed...
            if (!l.isPushed()) {
               // computes its true beginning stack size...
               l.inputStackTop = b.info == Edge.EXCEPTION ? 1 : start + b.info;

               // ...and pushes it onto the stack.
               l.markAsPushed();
               l.next = stack;
               stack = l;
            }

            b = b.next;
         }
      }

      return max;
   }
}

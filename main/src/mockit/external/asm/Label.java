/*
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package mockit.external.asm;

import javax.annotation.*;

/**
 * A label represents a position in the bytecode of a method. Labels are used for jump, goto, and switch instructions,
 * and for try catch blocks. A label designates the <i>instruction</i> that is just after. Note however that there can
 * be other elements between a label and the instruction it designates (such as other labels, stack map frames, line
 * numbers, etc.).
 */
public final class Label
{
   /**
    * Constants for the current status of a label.
    */
   interface Status
   {
      /**
       * Indicates if this label is only used for debug attributes. Such a label is not the start of a basic block, the
       * target of a jump instruction, or an exception handler. It can be safely ignored in control flow graph analysis
       * algorithms (for optimization purposes).
       */
      int DEBUG = 1;

      /**
       * Indicates if the position of this label is known.
       */
      int RESOLVED = 2;

      /**
       * Indicates if this basic block has been pushed in the basic block stack. See {@link MethodWriter#visitMaxStack}.
       */
      int PUSHED = 8;

      /**
       * Indicates if this label is the target of a jump instruction, or the start of an exception handler.
       */
      int TARGET = 16;

      /**
       * Indicates if a stack map frame must be stored for this label.
       */
      int STORE = 32;

      /**
       * Indicates if this label corresponds to a reachable basic block.
       */
      int REACHABLE = 64;

      /**
       * Indicates if this basic block ends with a JSR instruction.
       */
      int JSR = 128;

      /**
       * Indicates if this basic block ends with a RET instruction.
       */
      int RET = 256;

      /**
       * Indicates if this basic block is the start of a subroutine.
       */
      int SUBROUTINE = 512;

      /**
       * Indicates if this subroutine basic block has been visited by a visitSubroutine(null, ...) call.
       */
      int VISITED = 1024;

      /**
       * Indicates if this subroutine basic block has been visited by a visitSubroutine(!null, ...) call.
       */
      int VISITED2 = 2048;
   }

   /**
    * Field used to associate user information to a label.
    */
   @Nullable public Object info;

   /**
    * Flags that indicate the {@link Status} of this label.
    */
   int status;

   /**
    * The line number corresponding to this label, if known.
    */
   @Nonnegative public int line;

   /**
    * The position of this label in the code, if known.
    */
   @Nonnegative public int position;

   /**
    * Number of forward references to this label, times two.
    */
   @Nonnegative private int referenceCount;

   /**
    * Information about forward references. Each forward reference is described by two consecutive integers in this
    * array: the first one is the position of the first byte of the bytecode instruction that contains the forward
    * reference, while the second is the position of the first byte of the forward reference itself. In fact the sign of
    * the first integer indicates if this reference uses 2 or 4 bytes, and its absolute value gives the position of the
    * bytecode instruction. This array is also used as a bit set to store the subroutines to which a basic block
    * belongs. This information is needed in {@link MethodWriter#visitMaxStack}, after all forward references have been
    * resolved. Hence the same array can be used for both purposes without problems.
    */
   private int[] srcAndRefPositions;

   // Fields for the control flow and data flow graph analysis algorithms (used to compute the maximum stack size or the
   // stack map frames). A control flow graph contains one node per "basic block", and one edge per "jump" from one
   // basic block to another. Each node (i.e., each basic block) is represented by the Label object that corresponds to
   // the first instruction of this basic block. Each node also stores the list of its successors in the graph, as a
   // linked list of Edge objects.
   //
   // The control flow analysis algorithms used to compute the maximum stack size or the stack map frames are similar
   // and use two steps. The first step, during the visit of each instruction, builds information about the state of
   // the local variables and the operand stack at the end of each basic block, called the "output frame",
   // <i>relatively</i> to the frame state at the beginning of the basic block, which is called the "input frame", and
   // which is <i>unknown</i> during this step. The second step, in {@link MethodWriter#visitMaxStack}, is a fix point
   // algorithm that computes information about the input frame of each basic block, from the input state of the first
   // basic block (known from the method signature), and by the using the previously computed relative output frames.
   //
   // The algorithm used to compute the maximum stack size only computes the relative output and absolute input stack
   // heights, while the algorithm used to compute stack map frames computes relative output frames and absolute input
   // frames.

   /**
    * Start of the output stack relatively to the input stack. The exact semantics of this field depends on the
    * algorithm that is used.
    * <p/>
    * When only the maximum stack size is computed, this field is the number of elements in the input stack.
    * <p/>
    * When the stack map frames are completely computed, this field is the offset of the first output stack element
    * relatively to the top of the input stack. This offset is always negative or null. A null offset means that the
    * output stack must be appended to the input stack. A -n offset means that the first n output stack elements must
    * replace the top n input stack elements, and that the other elements must be appended to the input stack.
    */
   @Nonnegative int inputStackTop;

   /**
    * Maximum height reached by the output stack, relatively to the top of the input stack. This maximum is always
    * positive or null.
    */
   @Nonnegative int outputStackMax;

   /**
    * Information about the input and output stack map frames of this basic block.
    * This field is only used when {@link ClassWriter#computeFrames} option is used.
    */
   Frame frame;

   /**
    * The successor of this label, in the order they are visited. This linked list does not include labels used for
    * debug info only. If {@link ClassWriter#computeFrames} option is used then, in addition, it does not contain
    * successive labels that denote the same bytecode position (in this case only the first label appears in this list).
    */
   @Nullable Label successor;

   /**
    * The successors of this node in the control flow graph. These successors are stored in a linked list of
    * {@link Edge Edge} objects, linked to each other by their {@link Edge#next} field.
    */
   Edge successors;

   /**
    * The next basic block in the basic block stack. This stack is used in the main loop of the fix point algorithm used
    * in the second step of the control flow analysis algorithms. It is also used in {@link #visitSubroutine} to avoid
    * using a recursive method.
    *
    * @see MethodWriter#visitMaxStack
    */
   @Nullable Label next;

   public boolean isDebug() { return (status & Status.DEBUG) != 0; }
   boolean isResolved()     { return (status & Status.RESOLVED) != 0; }
   boolean isPushed()       { return (status & Status.PUSHED) != 0; }
   boolean isTarget()       { return (status & Status.TARGET) != 0; }
   boolean isStoringFrame() { return (status & Status.STORE) != 0; }
   boolean isReachable()    { return (status & Status.REACHABLE) != 0; }
   boolean isJSR()          { return (status & Status.JSR) != 0; }
   boolean isRET()          { return (status & Status.RET) != 0; }
   boolean isVisited()      { return (status & Status.VISITED) != 0; }

   void markAsDebug()              { status |= Status.DEBUG; }
   void markAsResolved()           { status |= Status.RESOLVED; }
   void markAsPushed()             { status |= Status.PUSHED; }
   void markAsTarget()             { status |= Status.TARGET; }
   void markAsStoringFrame()       { status |= Status.STORE; }
   void markAsReachable()          { status |= Status.REACHABLE; }
   void markAsJSR()                { status |= Status.JSR; }
   void markAsEndingWithRET()      { status |= Status.RET; }
   void markAsVisitedSubroutine()  { status |= Status.VISITED; }

   void markAsTarget(@Nonnull Label target) { status |= target.status & Status.TARGET; }

   boolean markAsSubroutine() {
      if ((status & Status.SUBROUTINE) == 0) {
         status |= Status.SUBROUTINE;
         return true;
      }

      return false;
   }

   boolean markAsVisitedSubroutine2() {
      if ((status & Status.VISITED2) != 0) {
         return true;
      }

      status |= Status.VISITED2;
      return false;
   }

   void markThisAndSuccessorsAsNotVisitedBySubroutine() {
      Label label = this;

      while (label != null) {
         label.status &= ~Status.VISITED2;
         label = label.successor;
      }
   }

   // ------------------------------------------------------------------------
   // Methods to compute offsets and to manage forward references
   // ------------------------------------------------------------------------

   /**
    * Puts a reference to this label in the bytecode of a method. If the position of the label is known, the offset is
    * computed and written directly. Otherwise, a null offset is written and a new forward reference is declared for
    * this label.
    *
    * @param out        the bytecode of the method.
    * @param source     the position of first byte of the bytecode instruction that contains this label.
    * @param wideOffset <tt>true</tt> if the reference must be stored in 4 bytes, or <tt>false</tt> if it must be stored
    *                   with 2 bytes.
    * @throws IllegalArgumentException if this label has not been created by the given code writer.
    */
   void put(@Nonnull ByteVector out, @Nonnegative int source, boolean wideOffset) {
      if (!isResolved()) {
         if (wideOffset) {
            addReference(-1 - source, out.length);
            out.putInt(-1);
         }
         else {
            addReference(source, out.length);
            out.putShort(-1);
         }
      }
      else {
         if (wideOffset) {
            out.putInt(position - source);
         }
         else {
            out.putShort(position - source);
         }
      }
   }

   /**
    * Adds a forward reference to this label. This method must be called only for a true forward reference, i.e. only if
    * this label is not resolved yet. For backward references, the offset of the reference can be, and must be, computed
    * and stored directly.
    *
    * @param sourcePosition    the position of the referencing instruction. This position will be used to compute the
    *                          offset of this forward reference.
    * @param referencePosition the position where the offset for this forward reference must be stored.
    */
   private void addReference(@Nonnegative int sourcePosition, @Nonnegative int referencePosition) {
      if (srcAndRefPositions == null) {
         srcAndRefPositions = new int[6];
      }

      if (referenceCount >= srcAndRefPositions.length) {
         int[] a = new int[srcAndRefPositions.length + 6];
         System.arraycopy(srcAndRefPositions, 0, a, 0, srcAndRefPositions.length);
         srcAndRefPositions = a;
      }

      srcAndRefPositions[referenceCount++] = sourcePosition;
      srcAndRefPositions[referenceCount++] = referencePosition;
   }

   /**
    * Resolves all forward references to this label. This method must be called when this label is added to the bytecode
    * of the method, i.e. when its position becomes known. This method fills in the blanks that where left in the
    * bytecode by each forward reference previously added to this label.
    *
    * @param methodBytecode bytecode of the method containing this label
    */
   void resolve(@Nonnull ByteVector methodBytecode) {
      markAsResolved();

      byte[] data = methodBytecode.data;
      int position = methodBytecode.length;
      this.position = position;
      int[] srcAndRefPos = srcAndRefPositions;

      for (int i = 0, refCount = referenceCount; i < refCount; ) {
         int source = srcAndRefPos[i++];
         int reference = srcAndRefPos[i++];
         int offset;

         if (source >= 0) {
            offset = position - source;
         }
         else {
            offset = position + source + 1;
            data[reference++] = (byte) (offset >>> 24);
            data[reference++] = (byte) (offset >>> 16);
         }

         data[reference++] = (byte) (offset >>> 8);
         data[reference] = (byte) offset;
      }
   }

   /**
    * Returns the first label of the series to which this label belongs. For an isolated label or for the first label in
    * a series of successive labels, this method returns the label itself. For other labels it returns the first label
    * of the series.
    *
    * @return the first label of the series to which this label belongs.
    */
   @Nonnull
   Label getFirst() {
      return frame == null ? this : frame.owner;
   }

   // ------------------------------------------------------------------------
   // Methods related to subroutines
   // ------------------------------------------------------------------------

   /**
    * Returns true is this basic block belongs to the given subroutine.
    *
    * @param id a subroutine id.
    */
   private boolean inSubroutine(long id) {
      //noinspection SimplifiableIfStatement
      if (isVisited()) {
         return (srcAndRefPositions[(int) (id >>> 32)] & (int) id) != 0;
      }

      return false;
   }

   /**
    * Returns true if this basic block and the given one belong to a common subroutine.
    *
    * @param block another basic block.
    */
   private boolean inSameSubroutine(@Nonnull Label block) {
      if (!isVisited() || !block.isVisited()) {
         return false;
      }

      for (int i = 0; i < srcAndRefPositions.length; ++i) {
         if ((srcAndRefPositions[i] & block.srcAndRefPositions[i]) != 0) {
            return true;
         }
      }

      return false;
   }

   /**
    * Marks this basic block as belonging to the given subroutine.
    *
    * @param id            a subroutine id.
    * @param nbSubroutines the total number of subroutines in the method.
    */
   private void addToSubroutine(long id, @Nonnegative int nbSubroutines) {
      if (!isVisited()) {
         markAsVisitedSubroutine();
         srcAndRefPositions = new int[nbSubroutines / 32 + 1];
      }

      srcAndRefPositions[(int) (id >>> 32)] |= (int) id;
   }

   /**
    * Finds the basic blocks that belong to a given subroutine, and marks these blocks as belonging to this subroutine.
    * This method follows the control flow graph to find all the blocks that are reachable from the current block
    * WITHOUT following any JSR target.
    *
    * @param JSR a JSR block that jumps to this subroutine. If this JSR is not null it is added to the successor of the
    *            RET blocks found in the subroutine.
    * @param id the id of this subroutine.
    * @param nbSubroutines the total number of subroutines in the method.
    */
   void visitSubroutine(@Nullable Label JSR, long id, @Nonnegative int nbSubroutines) {
      // User managed stack of labels, to avoid using a recursive method (recursivity can lead to stack overflow with
      // very large methods).
      Label stack = this;

      while (stack != null) {
         // Removes a label l from the stack.
         Label l = stack;
         stack = l.next;
         l.next = null;

         if (JSR != null) {
            if (l.markAsVisitedSubroutine2()) {
               continue;
            }

            // Adds JSR to the successors of l, if it is a RET block.
            if (l.isRET()) {
               if (!l.inSameSubroutine(JSR)) {
                  Edge e = new Edge(l.inputStackTop, JSR.successors.successor);
                  e.next = l.successors;
                  l.successors = e;
               }
            }
         }
         else {
            // If the l block already belongs to subroutine 'id', continue.
            if (l.inSubroutine(id)) {
               continue;
            }

            // Marks the l block as belonging to subroutine 'id'.
            l.addToSubroutine(id, nbSubroutines);
         }

         // Pushes each successor of l on the stack, except JSR targets.
         Edge e = l.successors;

         while (e != null) {
            // If the l block is a JSR block, then 'l.successors.next' leads to the JSR target
            // (see {@link #visitJumpInsn}) and must therefore not be followed.
            if (!l.isJSR() || e != l.successors.next) {
               // Pushes e.successor on the stack if it not already added.
               if (e.successor.next == null) {
                  e.successor.next = stack;
                  stack = e.successor;
               }
            }

            e = e.next;
         }
      }
   }

   // ------------------------------------------------------------------------
   // Overridden Object methods
   // ------------------------------------------------------------------------

   /**
    * Returns a string representation of this label.
    */
   @Override
   public String toString() {
      return "L" + System.identityHashCode(this);
   }
}

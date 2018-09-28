package mockit.asm;

import javax.annotation.*;

import mockit.asm.controlFlowGraph.*;
import mockit.asm.frames.*;

/**
 * A label represents a position in the bytecode of a method.
 * Labels are used for jump, goto, and switch instructions, and for try catch blocks.
 * A label designates the <em>instruction</em> that is just after.
 * Note however that there can be other elements between a label and the instruction it designates (such as other labels, stack map frames,
 * line numbers, etc.).
 */
public final class Label
{
   /**
    * Constants for the current status of a label.
    */
   interface Status {
      /**
       * Indicates if this label is only used for debug attributes. Such a label is not the start of a basic block, the target of a jump
       * instruction, or an exception handler.
       * It can be safely ignored in control flow graph analysis algorithms (for optimization purposes).
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
   }

   /**
    * Field used to associate user information to a label.
    */
   @Nullable public Object info;

   /**
    * Flags that indicate the {@link Status Status} of this label.
    */
   private int status;

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
    * Information about forward references.
    * Each forward reference is described by two consecutive integers in this array: the first one is the position of the first byte of the
    * bytecode instruction that contains the forward reference, while the second is the position of the first byte of the forward reference
    * itself.
    * In fact the sign of the first integer indicates if this reference uses 2 or 4 bytes, and its absolute value gives the position of the
    * bytecode instruction.
    * This array is also used as a bit set to store the subroutines to which a basic block belongs.
    * This information is needed in {@link MethodWriter#visitMaxStack}, after all forward references have been resolved.
    * Hence the same array can be used for both purposes without problems.
    */
   private int[] srcAndRefPositions;

   // Fields for the control flow and data flow graph analysis algorithms (used to compute the maximum stack size or the stack map frames).
   // A control flow graph contains one node per "basic block", and one edge per "jump" from one basic block to another.
   // Each node (i.e., each basic block) is represented by the Label object that corresponds to the first instruction of this basic block.
   // Each node also stores the list of its successors in the graph, as a linked list of Edge objects.
   //
   // The control flow analysis algorithms used to compute the maximum stack size or the stack map frames are similar and use two steps.
   // The first step, during the visit of each instruction, builds information about the state of the local variables and the operand stack
   // at the end of each basic block, called the "output frame", <em>relatively</em> to the frame state at the beginning of the basic block,
   // which is called the "input frame", and which is <em>unknown</em> during this step.
   // The second step, in {@link MethodWriter#visitMaxStack}, is a fix point algorithm that computes information about the input frame of
   // each basic block, from the input state of the first basic block (known from the method signature), and by the using the previously
   // computed relative output frames.
   //
   // The algorithm used to compute the maximum stack size only computes the relative output and absolute input stack heights, while the
   // algorithm used to compute stack map frames computes relative output frames and absolute input frames.

   /**
    * Start of the output stack relatively to the input stack. The exact semantics of this field depends on the algorithm that is used.
    * <p/>
    * When only the maximum stack size is computed, this field is the number of elements in the input stack.
    * <p/>
    * When the stack map frames are completely computed, this field is the offset of the first output stack element relatively to the top of
    * the input stack. This offset is always negative or null. A null offset means that the output stack must be appended to the input
    * stack. A -n offset means that the first n output stack elements must replace the top n input stack elements, and that the other
    * elements must be appended to the input stack.
    */
   @Nonnegative int inputStackTop;

   /**
    * Maximum height reached by the output stack, relatively to the top of the input stack. This maximum is always positive or null.
    */
   @Nonnegative int outputStackMax;

   /**
    * Information about the input and output stack map frames of this basic block. This field is only used for classfiles of version 1.7+.
    */
   Frame frame;

   /**
    * The successor of this label, in the order they are visited. This linked list does not include labels used for debug info only.
    * If the classfile being read is of version 1.7+ then, in addition, it does not contain successive labels that denote the same bytecode
    * position (in this case only the first label appears in this list).
    */
   @Nullable Label successor;

   /**
    * The successors of this node in the control flow graph.
    * These successors are stored in a linked list of {@link Edge Edge} objects, linked to each other by their {@link Edge#next} field.
    */
   Edge successors;

   /**
    * The next basic block in the basic block stack.
    * This stack is used in the main loop of the fix point algorithm used in the second step of the control flow analysis algorithms.
    *
    * @see MethodWriter#visitMaxStack
    */
   @Nullable Label next;

   /**
    * Returns the {@link #frame} this basic block belongs to, if any.
    */
   public Frame getFrame() { return frame; }

   /**
    * Sets the {@link #frame} this basic block belongs to, if any.
    */
   public void setFrame(Frame frame) { this.frame = frame; }

   public boolean isDebug()  { return (status & Status.DEBUG) != 0; }
   boolean isResolved()      { return (status & Status.RESOLVED) != 0; }
   public boolean isPushed() { return (status & Status.PUSHED) != 0; }
   public boolean isTarget() { return (status & Status.TARGET) != 0; }
   boolean isStoringFrame()  { return (status & Status.STORE) != 0; }
   boolean isReachable()     { return (status & Status.REACHABLE) != 0; }

   void markAsDebug()               { status |= Status.DEBUG; }
   private void markAsResolved()    { status |= Status.RESOLVED; }
   public void markAsPushed()       { status |= Status.PUSHED; }
   public void markAsTarget()       { status |= Status.TARGET; }
   public void markAsStoringFrame() { status |= Status.STORE; }
   public void markAsReachable()    { status |= Status.REACHABLE; }

   public void markAsTarget(@Nonnull Label target) { status |= target.status & Status.TARGET; }

   /**
    * Puts a reference to this label in the bytecode of a method.
    * If the position of the label is known, the offset is computed and written directly.
    * Otherwise, a null offset is written and a new forward reference is declared for this label.
    *
    * @param out the bytecode of the method
    * @param source the position of first byte of the bytecode instruction that contains this label
    * @param wideOffset <tt>true</tt> if the reference must be stored in 4 bytes, or <tt>false</tt> if it must be stored with 2 bytes
    * @throws IllegalArgumentException if this label has not been created by the given code writer
    */
   void put(@Nonnull ByteVector out, @Nonnegative int source, boolean wideOffset) {
      if (isResolved()) {
         int reference = position - source;

         if (wideOffset) {
            out.putInt(reference);
         }
         else {
            out.putShort(reference);
         }
      }
      else {
         if (wideOffset) {
            addReference(-1 - source, out.length);
            out.putInt(-1);
         }
         else {
            addReference(source, out.length);
            out.putShort(-1);
         }
      }
   }

   /**
    * Adds a forward reference to this label.
    * This method must be called only for a true forward reference, i.e. only if this label is not resolved yet.
    * For backward references, the offset of the reference can be, and must be, computed and stored directly.
    *
    * @param sourcePosition the position of the referencing instruction, which will be used to compute the offset of this forward reference
    * @param referencePosition the position where the offset for this forward reference must be stored
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
    * Resolves all forward references to this label.
    * This method must be called when this label is added to the bytecode of the method, i.e. when its position becomes known.
    * This method fills in the blanks that where left in the bytecode by each forward reference previously added to this label.
    *
    * @param methodBytecode bytecode of the method containing this label
    */
   @SuppressWarnings("NumericCastThatLosesPrecision")
   public void resolve(@Nonnull ByteVector methodBytecode) {
      markAsResolved();

      byte[] data = methodBytecode.data;
      int pos = methodBytecode.length;
      position = pos;
      int[] srcAndRefPos = srcAndRefPositions;

      for (int i = 0, refCount = referenceCount; i < refCount; i += 2) {
         int source = srcAndRefPos[i];
         int reference = srcAndRefPos[i + 1];
         int offset;

         if (source >= 0) {
            offset = pos - source;
         }
         else {
            offset = pos + source + 1;
            data[reference++] = (byte) (offset >>> 24);
            data[reference++] = (byte) (offset >>> 16);
         }

         data[reference++] = (byte) (offset >>> 8);
         data[reference] = (byte) offset;
      }
   }

   /**
    * Returns the first label of the series to which this label belongs.
    * For an isolated label or for the first label in a series of successive labels, returns the label itself.
    * For other labels, returns the first label of the series.
    *
    * @return the first label of the series to which this label belongs
    */
   @Nonnull
   public Label getFirst() {
      return frame == null ? this : frame.owner;
   }

   /**
    * Returns the {@link #inputStackTop}.
    */
   public int getInputStackTop() { return inputStackTop; }

   /**
    * Sets the {@link #inputStackTop}.
    */
   public void setInputStackTop(int inputStackTop) { this.inputStackTop = inputStackTop; }

   @Nonnegative
   public int decrementInputStackTop() { return --inputStackTop; }

   @Nonnegative
   public void decrementInputStackTop(@Nonnegative int amount) { inputStackTop -= amount; }

   /**
    * Returns the {@link #outputStackMax}.
    */
   public int getOutputStackMaxSize() { return outputStackMax; }

   /**
    * Sets the {@link #outputStackMax}.
    */
   public void setOutputStackMaxSize(@Nonnegative int outputStackMax) { this.outputStackMax = outputStackMax; }

   /**
    * Sets this label's {@link #successor}.
    */
   public void setSuccessor(@Nullable Label successor) { this.successor = successor; }

   /**
    * Returns this node's {@link #successors}.
    */
   public Edge getSuccessors() { return successors; }

   @Nullable
   public Label setSuccessors(@Nonnull Edge edge) {
      edge.setNext(successors);
      successors = edge;
      return successor;
   }

   /**
    * Returns the label to the {@link #next} basic block, if any.
    */
   @Nullable
   public Label getNext() { return next; }

   /**
    * Sets the label to the {@link #next} basic block, if any.
    */
   public void setNext(@Nullable Label next) { this.next = next; }

   @Override
   public String toString() {
      return "L" + System.identityHashCode(this);
   }
}

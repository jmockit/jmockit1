package mockit.asm.controlFlowGraph;

import javax.annotation.*;

import mockit.asm.*;

/**
 * An edge in the control flow graph of a method body. See {@link Label}.
 */
public final class Edge
{
   /**
    * Denotes a normal control flow graph edge.
    */
   static final int NORMAL = 0;

   /**
    * Denotes a control flow graph edge corresponding to an exception handler. More precisely any {@link Edge} whose {@link #info} is
    * strictly positive corresponds to an exception handler.
    * The actual value of {@link #info} is the index, in the type table of the classfile being written, of the exception that is caught.
    */
   public static final int EXCEPTION = 0x7FFFFFFF;

   /**
    * Information about this control flow graph edge.
    * For classfiles older than 1.7, this field is the (relative) stack size in the basic block from which this edge originates.
    * This size is equal to the stack size at the "jump" instruction to which this edge corresponds, relatively to the stack size at the
    * beginning of the originating basic block.
    * For 1.7+ classfiles, this field is the kind of this control flow graph edge (i.e. NORMAL or EXCEPTION).
    */
   final int info;

   /**
    * The successor block of the basic block from which this edge originates.
    */
   final Label successor;

   /**
    * The next edge in the list of successors of the originating basic block. See {@link Label#successors}.
    */
   Edge next;

   public Edge(int info, @Nonnull Label successor) {
      this.info = info;
      this.successor = successor;
   }

   /**
    * Sets the {@link #next} edge.
    */
   public void setNext(Edge next) { this.next = next; }
}

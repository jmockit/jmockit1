package mockit.asm.exceptionHandling;

import javax.annotation.*;

import mockit.asm.constantPool.*;
import mockit.asm.controlFlow.*;
import mockit.asm.util.*;

public final class ExceptionHandling
{
   @Nonnull private final ConstantPoolGeneration cp;

   /**
    * Number of elements in the exception handler list.
    */
   @Nonnegative private int handlerCount;

   /**
    * The first element in the exception handler list.
    */
   @Nullable private ExceptionHandler firstExceptionHandler;

   /**
    * The last element in the exception handler list.
    */
   @Nullable private ExceptionHandler lastExceptionHandler;

   public ExceptionHandling(@Nonnull ConstantPoolGeneration cp) { this.cp = cp; }

   public void addHandler(@Nonnull Label start, @Nonnull Label end, @Nonnull Label handler, @Nullable String type) {
      handlerCount++;

      int handlerType = type != null ? cp.newClass(type) : 0;
      ExceptionHandler h = new ExceptionHandler(start, end, handler, type, handlerType);

      if (lastExceptionHandler == null) {
         firstExceptionHandler = h;
      }
      else {
         lastExceptionHandler.next = h;
      }

      lastExceptionHandler = h;
   }

   public void countNumberOfHandlers() {
      ExceptionHandler exceptionHandler = firstExceptionHandler;
      handlerCount = 0;

      while (exceptionHandler != null) {
         handlerCount++;
         exceptionHandler = exceptionHandler.next;
      }
   }

   public void completeControlFlowGraphWithExceptionHandlerBlocksFromComputedFrames() {
      ExceptionHandler exceptionHandler = firstExceptionHandler;

      while (exceptionHandler != null) {
         Label handler = exceptionHandler.handler.getFirst();
         Label start = exceptionHandler.start.getFirst();
         Label end = exceptionHandler.end.getFirst();

         // Computes the kind of the edges to 'handler'.
         String catchType = exceptionHandler.getCatchTypeDesc();
         int kindOfEdge = FrameTypeMask.OBJECT | cp.addNormalType(catchType);

         // 'handler' is an exception handler.
         handler.markAsTarget();

         addHandlerLabelAsSuccessor(kindOfEdge, handler, start, end);
         exceptionHandler = exceptionHandler.next;
      }
   }

   public void completeControlFlowGraphWithExceptionHandlerBlocks() {
      ExceptionHandler exceptionHandler = firstExceptionHandler;

      while (exceptionHandler != null) {
         addHandlerLabelAsSuccessor(Edge.EXCEPTION, exceptionHandler.handler, exceptionHandler.start, exceptionHandler.end);
         exceptionHandler = exceptionHandler.next;
      }
   }

   // Adds 'handler' as a successor of labels between 'start' and 'end'.
   private static void addHandlerLabelAsSuccessor(int kindOfEdge, @Nonnull Label handler, @Nonnull Label start, @Nonnull Label end) {
      while (start != end) {
         Edge edge = new Edge(kindOfEdge, handler);
         //noinspection ConstantConditions
         start = start.setSuccessors(edge);
      }
   }

   // Removes the start-end range from the exception handlers.
   public void removeStartEndRange(@Nonnull Label start, @Nullable Label end) {
      firstExceptionHandler = ExceptionHandler.remove(firstExceptionHandler, start, end);
   }

   public boolean hasHandlers() { return handlerCount > 0; }

   @Nonnegative
   public int getSize() { return 8 * handlerCount; }

   public void put(@Nonnull ByteVector out) {
      out.putShort(handlerCount);

      if (handlerCount > 0) {
         ExceptionHandler h = firstExceptionHandler;

         while (h != null) {
            h.put(out);
            h = h.next;
         }
      }
   }
}

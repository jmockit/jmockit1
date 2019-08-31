package mockit.asm.exceptionHandling;

import java.util.*;
import javax.annotation.*;

import mockit.asm.constantPool.*;
import mockit.asm.controlFlow.*;
import mockit.asm.util.*;

public final class ExceptionHandling
{
   @Nonnull private final List<ExceptionHandler> handlers;
   @Nonnull private final ConstantPoolGeneration cp;

   public ExceptionHandling(@Nonnull ConstantPoolGeneration cp) {
      handlers = new ArrayList<>();
      this.cp = cp;
   }

   public void addHandler(@Nonnull Label start, @Nonnull Label end, @Nonnull Label handler, @Nullable String type) {
      int handlerType = type == null ? 0 : cp.newClass(type);
      handlers.add(new ExceptionHandler(start, end, handler, type, handlerType));
   }

   public void completeControlFlowGraphWithExceptionHandlerBlocksFromComputedFrames() {
      for (ExceptionHandler exceptionHandler : handlers) {
         Label handler = exceptionHandler.handler.getFirst();
         Label start = exceptionHandler.start.getFirst();
         Label end = exceptionHandler.end.getFirst();

         // Computes the kind of the edges to 'handler'.
         String catchType = exceptionHandler.getCatchTypeDesc();
         int kindOfEdge = FrameTypeMask.OBJECT | cp.addNormalType(catchType);

         // 'handler' is an exception handler.
         handler.markAsTarget();

         addHandlerLabelAsSuccessor(kindOfEdge, handler, start, end);
      }
   }

   public void completeControlFlowGraphWithExceptionHandlerBlocks() {
      for (ExceptionHandler exceptionHandler : handlers) {
         addHandlerLabelAsSuccessor(Edge.EXCEPTION, exceptionHandler.handler, exceptionHandler.start, exceptionHandler.end);
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

   public boolean hasHandlers() { return !handlers.isEmpty(); }

   @Nonnegative
   public int getSize() { return 8 * handlers.size(); }

   public void put(@Nonnull ByteVector out) {
      out.putShort(handlers.size());

      for (ExceptionHandler exceptionHandler : handlers) {
         exceptionHandler.put(out);
      }
   }
}
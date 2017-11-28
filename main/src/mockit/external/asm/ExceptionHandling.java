package mockit.external.asm;

import javax.annotation.*;

final class ExceptionHandling
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

   ExceptionHandling(@Nonnull ConstantPoolGeneration cp) { this.cp = cp; }

   void addHandler(@Nonnull Label start, @Nonnull Label end, @Nonnull Label handler, @Nullable String type) {
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

   void countNumberOfHandlers() {
      ExceptionHandler exceptionHandler = firstExceptionHandler;
      handlerCount = 0;

      while (exceptionHandler != null) {
         handlerCount++;
         exceptionHandler = exceptionHandler.next;
      }
   }

   void completeControlFlowGraphWithExceptionHandlerBlocksFromComputedFrames() {
      ExceptionHandler exceptionHandler = firstExceptionHandler;

      while (exceptionHandler != null) {
         Label currentLabel = exceptionHandler.start.getFirst();
         Label handlerLabel = exceptionHandler.handler.getFirst();
         Label endLabel = exceptionHandler.end.getFirst();

         // Computes the kind of the edges to 'handlerLabel'.
         String catchType = exceptionHandler.desc == null ? "java/lang/Throwable" : exceptionHandler.desc;
         int kind = Frame.TypeMask.OBJECT | cp.addType(catchType);

         // handlerLabel is an exception handler.
         handlerLabel.markAsTarget();

         // Adds 'handlerLabel' as a successor of labels between 'start' and 'end'.
         while (currentLabel != endLabel) {
            // Creates an edge to 'handlerLabel'.
            Edge successor = new Edge(kind, handlerLabel);

            // Adds it to the successors of 'currentLabel'.
            //noinspection ConstantConditions
            successor.next = currentLabel.successors;
            currentLabel.successors = successor;

            // Goes to the next label.
            currentLabel = currentLabel.successor;
         }

         exceptionHandler = exceptionHandler.next;
      }
   }

   // Removes the start-end range from the exception handlers.
   void removeStartEndRange(@Nonnull Label start, @Nullable Label end) {
      firstExceptionHandler = ExceptionHandler.remove(firstExceptionHandler, start, end);
   }

   void completeControlFlowGraphWithExceptionHandlerBlocks() {
      ExceptionHandler exceptionHandler = firstExceptionHandler;

      while (exceptionHandler != null) {
         Label currentLabel = exceptionHandler.start;
         Label handlerLabel = exceptionHandler.handler;
         Label endLabel = exceptionHandler.end;

         // Adds 'handlerLabel' as a successor of labels between 'start' and 'end'.
         while (currentLabel != endLabel) {
            // Creates an edge to 'handlerLabel'.
            Edge edge = new Edge(Edge.EXCEPTION, handlerLabel);

            // Adds it to the successors of 'currentLabel'.
            //noinspection ConstantConditions
            if (!currentLabel.isJSR()) {
               edge.next = currentLabel.successors;
               currentLabel.successors = edge;
            }
            else {
               // If currentLabel is a JSR block, adds edge after the first two edges to preserve the hypothesis about
               // JSR block successors order (see {@link #visitJumpInsn}).
               edge.next = currentLabel.successors.next.next;
               currentLabel.successors.next.next = edge;
            }

            // Goes to the next label.
            currentLabel = currentLabel.successor;
         }

         exceptionHandler = exceptionHandler.next;
      }
   }

   boolean hasHandlers() { return handlerCount > 0; }

   @Nonnegative
   int getSize() { return 8 * handlerCount; }

   void put(@Nonnull ByteVector out) {
      out.putShort(handlerCount);

      if (handlerCount > 0) {
         ExceptionHandler h = firstExceptionHandler;

         while (h != null) {
            out.putShort(h.start.position).putShort(h.end.position);
            out.putShort(h.handler.position).putShort(h.type);
            h = h.next;
         }
      }
   }
}

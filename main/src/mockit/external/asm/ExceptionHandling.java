package mockit.external.asm;

final class ExceptionHandling
{
   private final ConstantPoolGeneration cp;

   /**
    * Number of elements in the exception handler list.
    */
   private int handlerCount;

   /**
    * The first element in the exception handler list.
    */
   private ExceptionHandler firstExceptionHandler;

   /**
    * The last element in the exception handler list.
    */
   private ExceptionHandler lastExceptionHandler;

   ExceptionHandling(ConstantPoolGeneration cp) { this.cp = cp; }

   void addHandler(Label start, Label end, Label handler, String type) {
      ++handlerCount;

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
         Label l = exceptionHandler.start.getFirst();
         Label h = exceptionHandler.handler.getFirst();
         Label e = exceptionHandler.end.getFirst();

         // Computes the kind of the edges to 'h'.
         String t = exceptionHandler.desc == null ? "java/lang/Throwable" : exceptionHandler.desc;
         int kind = Frame.TypeMask.OBJECT | cp.addType(t);

         // h is an exception handler.
         h.markAsTarget();

         // Adds 'h' as a successor of labels between 'start' and 'end'.
         while (l != e) {
            // Creates an edge to 'h'.
            Edge b = new Edge(kind, h);

            // Adds it to the successors of 'l'.
            b.next = l.successors;
            l.successors = b;

            // Goes to the next label.
            l = l.successor;
         }

         exceptionHandler = exceptionHandler.next;
      }
   }

   // Removes the start-end range from the exception handlers.
   void removeStartEndRange(Label start, Label end) {
      firstExceptionHandler = ExceptionHandler.remove(firstExceptionHandler, start, end);
   }

   void completeControlFlowGraphWithExceptionHandlerBlocks() {
      ExceptionHandler exceptionHandler = firstExceptionHandler;

      while (exceptionHandler != null) {
         Label l = exceptionHandler.start;
         Label h = exceptionHandler.handler;
         Label e = exceptionHandler.end;

         // Adds 'h' as a successor of labels between 'start' and 'end'.
         while (l != e) {
            // Creates an edge to 'h'.
            Edge b = new Edge(Edge.EXCEPTION, h);

            // Adds it to the successors of 'l'.
            if (!l.isJSR()) {
               b.next = l.successors;
               l.successors = b;
            }
            else {
               // If l is a JSR block, adds b after the first two edges to preserve the hypothesis about JSR block
               // successors order (see {@link #visitJumpInsn}).
               b.next = l.successors.next.next;
               l.successors.next.next = b;
            }

            // Goes to the next label.
            l = l.successor;
         }

         exceptionHandler = exceptionHandler.next;
      }
   }

   boolean hasHandlers() { return handlerCount > 0; }

   int getSize() { return 8 * handlerCount; }

   void put(ByteVector out) {
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

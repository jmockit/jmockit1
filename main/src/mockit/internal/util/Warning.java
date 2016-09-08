/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.util;

import java.io.*;
import javax.annotation.*;

public final class Warning
{
   private Warning() {}

   public static void display(@Nonnull String message)
   {
      PrintStream output = System.err;
      output.print("Warning: ");
      output.println(message);

      StackTrace stackTrace = new StackTrace();
      stackTrace.filter();

      String previousFileName = null;
      int previousLineNumber = 0;

      for (int i = 0, d = stackTrace.getDepth(); i < d; i++) {
         StackTraceElement ste = stackTrace.getElement(i);

         if (ste.getLineNumber() != previousLineNumber || !ste.getFileName().equals(previousFileName)) {
            output.print("\tat ");
            output.println(ste);
            previousFileName = ste.getFileName();
            previousLineNumber = ste.getLineNumber();
         }
      }
   }
}

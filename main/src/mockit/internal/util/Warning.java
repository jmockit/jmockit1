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
      output.print("\nWarning: ");
      output.println(message);

      StackTrace stackTrace = new StackTrace();
      stackTrace.filter();
      stackTrace.print(output);
   }
}

/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.reporting;

import java.util.*;
import java.util.regex.*;

import org.jetbrains.annotations.*;

import mockit.coverage.*;

public final class ListOfCallPoints
{
   @NotNull private static final String EOL = System.getProperty("line.separator");
   private static final Pattern LESS_THAN_CHAR = Pattern.compile("<");

   @NotNull private final StringBuilder content;

   public ListOfCallPoints() { content = new StringBuilder(100); }

   public void insertListOfCallPoints(@Nullable List<CallPoint> callPoints)
   {
      if (content.length() == 0) {
         content.append(EOL).append("      ");
      }

      if (callPoints == null) {
         content.append("  <ol style='display:none'></ol>").append(EOL).append("      ");
         return;
      }

      content.append("  <ol style='display:none'>").append(EOL);

      CallPoint currentCP = callPoints.get(0);
      appendTestMethod(currentCP.getStackTraceElement());
      appendRepetitionCountIfNeeded(currentCP);

      for (int i = 1, n = callPoints.size(); i < n; i++) {
         CallPoint nextCP = callPoints.get(i);
         StackTraceElement ste = nextCP.getStackTraceElement();

         if (nextCP.isSameTestMethod(currentCP)) {
            content.append(", ").append(ste.getLineNumber());
         }
         else {
            content.append("</li>").append(EOL);
            appendTestMethod(ste);
         }

         appendRepetitionCountIfNeeded(nextCP);
         currentCP = nextCP;
      }

      content.append("</li>").append(EOL).append("        </ol>").append(EOL).append("      ");
   }

   private void appendTestMethod(@NotNull StackTraceElement current)
   {
      content.append("          <li>");
      content.append(current.getClassName()).append('#');
      content.append(LESS_THAN_CHAR.matcher(current.getMethodName()).replaceFirst("&lt;")).append(": ");
      content.append(current.getLineNumber());
   }

   private void appendRepetitionCountIfNeeded(@NotNull CallPoint callPoint)
   {
      int repetitionCount = callPoint.getRepetitionCount();

      if (repetitionCount > 0) {
         content.append('x').append(1 + repetitionCount);
      }
   }

   @NotNull public String getContents()
   {
      String result = content.toString();
      content.setLength(0);
      return result;
   }
}

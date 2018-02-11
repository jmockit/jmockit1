/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.lines;

import java.io.*;
import java.util.*;
import javax.annotation.*;

import mockit.coverage.*;

public class LineSegmentData implements Serializable
{
   private static final long serialVersionUID = -6233980722802474992L;
   private static final int MAX_CALL_POINTS = Integer.parseInt(Configuration.getProperty("maxCallPoints", "10"));

   // Constant data:
   private boolean unreachable;
   private boolean empty;

   // Runtime data:
   @Nonnegative int executionCount;
   @Nullable private List<CallPoint> callPoints;

   public final void markAsUnreachable() { unreachable = true; }

   public final boolean isEmpty() { return empty; }
   final void markAsEmpty() { empty = true; }

   final boolean acceptsAdditionalCallPoints() {
      return callPoints == null || callPoints.size() < MAX_CALL_POINTS;
   }

   @Nonnegative
   final int registerExecution(@Nullable CallPoint callPoint) {
      int previousExecutionCount = executionCount++;

      if (callPoint != null) {
         addCallPoint(callPoint);
      }

      return previousExecutionCount;
   }

   private void addCallPoint(@Nonnull CallPoint callPoint) {
      if (callPoints == null) {
         callPoints = new ArrayList<CallPoint>(MAX_CALL_POINTS);
      }

      for (int i = callPoints.size() - 1; i >= 0; i--) {
         CallPoint previousCallPoint = callPoints.get(i);

         if (callPoint.isSameLineInTestCode(previousCallPoint)) {
            previousCallPoint.incrementRepetitionCount();
            return;
         }
      }

      callPoints.add(callPoint);
   }

   public final boolean containsCallPoints() { return callPoints != null; }
   @Nullable public final List<CallPoint> getCallPoints() { return callPoints; }

   @Nonnegative public final int getExecutionCount() { return executionCount; }
   final void setExecutionCount(@Nonnegative int executionCount) { this.executionCount = executionCount; }

   public final boolean isCovered() { return unreachable || !empty && executionCount > 0; }

   final void addExecutionCountAndCallPointsFromPreviousTestRun(@Nonnull LineSegmentData previousData) {
      executionCount += previousData.executionCount;

      if (previousData.callPoints != null) {
         if (callPoints != null) {
            callPoints.addAll(0, previousData.callPoints);
         }
         else {
            callPoints = previousData.callPoints;
         }
      }
   }
}

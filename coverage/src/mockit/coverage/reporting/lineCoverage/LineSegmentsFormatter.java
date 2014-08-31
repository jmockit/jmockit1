/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.reporting.lineCoverage;

import java.util.*;
import static java.lang.Character.*;

import mockit.coverage.*;
import mockit.coverage.lines.*;
import mockit.coverage.reporting.*;
import mockit.coverage.reporting.parsing.*;

import org.jetbrains.annotations.*;

final class LineSegmentsFormatter
{
   @Nullable private final ListOfCallPoints listOfCallPoints;
   @NotNull private final StringBuilder line;
   private int lineNumber;

   // Helper fields:
   @Nullable private LineElement element;
   private int segmentIndex;

   LineSegmentsFormatter(boolean withCallPoints, @NotNull StringBuilder line)
   {
      listOfCallPoints = withCallPoints ? new ListOfCallPoints() : null;
      this.line = line;
   }

   void formatSegments(@NotNull LineParser lineParser, @NotNull LineCoverageData lineData)
   {
      lineNumber = lineParser.getNumber();

      List<BranchCoverageData> branchData = lineData.getBranches();
      int numSegments = lineData.getNumberOfSegments();

      element = lineParser.getInitialElement().appendUntilNextCodeElement(line);

      segmentIndex = 0;
      appendUntilFirstElementAfterNextBranchingPoint(lineData);

      while (element != null && segmentIndex < numSegments) {
         LineSegmentData segmentData = segmentIndex == 0 ? lineData : branchData.get(segmentIndex - 1);
         element = element.appendUntilNextCodeElement(line);
         appendUntilFirstElementAfterNextBranchingPoint(segmentData);
      }

      line.append("</pre>");

      if (listOfCallPoints != null && lineData.containsCallPoints()) {
         line.append(listOfCallPoints.getContents());
      }
   }

   private void appendUntilFirstElementAfterNextBranchingPoint(@NotNull LineSegmentData segmentData)
   {
      if (element != null) {
         LineElement firstElement = element;
         element = element.findNextBranchingPoint();

         appendToFormattedLine(firstElement, segmentData);

         if (element != null && element.isBranchingElement()) {
            line.append(element.getText());
            element = element.getNext();
         }
      }
   }

   private void appendToFormattedLine(@NotNull LineElement firstElement, @NotNull LineSegmentData segmentData)
   {
      if (firstElement == element) {
         return;
      }

      appendStartTag(segmentData);
      firstElement.appendAllBefore(line, element);
      appendEndTag(segmentData);

      segmentIndex++;
   }

   private void appendStartTag(@NotNull LineSegmentData segmentData)
   {
      line.append("<span id='l").append(lineNumber).append('s').append(segmentIndex).append("' ");

      appendTooltipWithExecutionCounts(segmentData);

      if (segmentData.isCovered()) {
         if (segmentData.containsCallPoints()) {
            line.append("class='covered cp' onclick='showHide(this,").append(segmentIndex).append(")'>");
         }
         else {
            line.append("class='covered'>");
         }
      }
      else {
         line.append("class='uncovered'>");
      }
   }

   private void appendTooltipWithExecutionCounts(@NotNull LineSegmentData segmentData)
   {
      line.append("title='Executions: ").append(segmentData.getExecutionCount()).append("' ");
   }

   private void appendEndTag(@NotNull LineSegmentData segmentData)
   {
      int i = line.length() - 1;

      while (isWhitespace(line.charAt(i))) {
         i--;
      }

      line.insert(i + 1, "</span>");

      if (listOfCallPoints != null) {
         List<CallPoint> callPoints = segmentData.getCallPoints();
         listOfCallPoints.insertListOfCallPoints(callPoints);
      }
   }
}

/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.reporting.lineCoverage;

import java.util.*;
import javax.annotation.*;
import static java.lang.Character.*;

import mockit.coverage.*;
import mockit.coverage.lines.*;
import mockit.coverage.reporting.*;
import mockit.coverage.reporting.parsing.*;

final class LineSegmentsFormatter
{
   @Nullable private final ListOfCallPoints listOfCallPoints;
   @Nonnull private final StringBuilder formattedLine;

   // Helper fields:
   private int lineNumber;
   @Nonnegative private int segmentIndex;
   @Nullable private LineElement element;

   LineSegmentsFormatter(boolean withCallPoints, @Nonnull StringBuilder formattedLine) {
      listOfCallPoints = withCallPoints ? new ListOfCallPoints() : null;
      this.formattedLine = formattedLine;
   }

   void formatSegments(@Nonnull LineParser lineParser, @Nonnull LineCoverageData lineData) {
      lineNumber = lineParser.getNumber();

      List<BranchCoverageData> branchData = lineData.getBranches();
      int numSegments = lineData.getNumberOfSegments();

      element = lineParser.getInitialElement().appendUntilNextCodeElement(formattedLine);

      segmentIndex = 0;
      appendUntilNextBranchingPoint(lineData);

      while (element != null && segmentIndex < numSegments) {
         LineSegmentData segmentData = segmentIndex == 0 ? lineData : branchData.get(segmentIndex - 1);
         element = element.appendUntilNextCodeElement(formattedLine);
         appendUntilNextBranchingPoint(segmentData);
      }

      if (element != null) {
         element.appendAllBefore(formattedLine, null);
      }

      formattedLine.append("</pre>");

      if (listOfCallPoints != null) {
         formattedLine.append(listOfCallPoints.getContents());
      }
   }

   private void appendUntilNextBranchingPoint(@Nonnull LineSegmentData segmentData) {
      if (element != null) {
         LineElement firstElement = element;
         element = element.findNextBranchingPoint();

         appendToFormattedLine(segmentData, firstElement);

         if (element != null && element.isBranchingElement()) {
            formattedLine.append(element.getText());
            element = element.getNext();
         }
      }
   }

   private void appendToFormattedLine(@Nonnull LineSegmentData segmentData, @Nonnull LineElement firstElement) {
      if (firstElement != element) {
         appendStartTag(segmentData);
         firstElement.appendAllBefore(formattedLine, element);
         appendEndTag(segmentData);

         segmentIndex++;
      }
   }

   private void appendStartTag(@Nonnull LineSegmentData segmentData) {
      formattedLine.append("<span id='l").append(lineNumber).append('s').append(segmentIndex);
      formattedLine.append("' title='Executions: ").append(segmentData.getExecutionCount()).append("' ");

      if (segmentData.isCovered()) {
         if (segmentData.containsCallPoints()) {
            formattedLine.append("class='cvd cp' onclick='sh(this,").append(segmentIndex).append(")'>");
         }
         else {
            formattedLine.append("class='cvd'>");
         }
      }
      else {
         formattedLine.append("class='uncvd'>");
      }
   }

   private void appendEndTag(@Nonnull LineSegmentData segmentData) {
      int i = formattedLine.length() - 1;

      while (isWhitespace(formattedLine.charAt(i))) {
         i--;
      }

      formattedLine.insert(i + 1, "</span>");

      if (listOfCallPoints != null) {
         List<CallPoint> callPoints = segmentData.getCallPoints();
         listOfCallPoints.insertListOfCallPoints(callPoints);
      }
   }
}
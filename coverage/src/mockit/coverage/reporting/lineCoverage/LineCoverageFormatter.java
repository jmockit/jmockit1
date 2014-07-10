/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.reporting.lineCoverage;

import java.util.*;

import org.jetbrains.annotations.*;

import mockit.coverage.*;
import mockit.coverage.lines.*;
import mockit.coverage.reporting.*;
import mockit.coverage.reporting.parsing.*;

final class LineCoverageFormatter
{
   @NotNull private final StringBuilder formattedLine;
   @NotNull private final LineSegmentsFormatter segmentsFormatter;
   @Nullable private final ListOfCallPoints listOfCallPoints;

   LineCoverageFormatter(boolean withCallPoints)
   {
      formattedLine = new StringBuilder(200);
      segmentsFormatter = new LineSegmentsFormatter(withCallPoints, formattedLine);
      listOfCallPoints = withCallPoints ? new ListOfCallPoints() : null;
   }

   String format(@NotNull LineParser lineParser, @NotNull PerFileLineCoverage lineCoverageData)
   {
      formattedLine.setLength(0);
      formattedLine.append("<pre class='prettyprint");

      int line = lineParser.getNumber();
      LineCoverageData lineData = lineCoverageData.getLineData(line);

      if (lineData.containsBranches()) {
         formatLineWithMultipleSegments(lineParser, lineData);
      }
      else {
         formatLineWithSingleSegment(lineParser, lineData);
      }

      return formattedLine.toString();
   }

   private void formatLineWithMultipleSegments(@NotNull LineParser lineParser, @NotNull LineCoverageData lineData)
   {
      formattedLine.append(" jmp'>");
      segmentsFormatter.formatSegments(lineParser, lineData);
   }

   private void formatLineWithSingleSegment(@NotNull LineParser lineParser, @NotNull LineCoverageData lineData)
   {
      formattedLine.append(lineData.isCovered() ? " covered" : " uncovered");

      List<CallPoint> callPoints = lineData.getCallPoints();
      boolean lineWithCallPoints = listOfCallPoints != null && callPoints != null;

      if (lineWithCallPoints) {
         formattedLine.append(" cp' onclick='showHide(this)");
      }

      formattedLine.append("' id='l").append(lineParser.getNumber()).append("s0'>");
      formattedLine.append(lineParser.getInitialElement().toString()).append("</pre>");

      if (lineWithCallPoints) {
         listOfCallPoints.insertListOfCallPoints(callPoints);
         formattedLine.append(listOfCallPoints.getContents());
      }
   }
}

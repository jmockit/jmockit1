/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.reporting.pathCoverage;

import java.io.*;
import java.util.*;

import org.jetbrains.annotations.*;

import mockit.coverage.paths.*;

final class PathCoverageFormatter
{
   @NotNull private final PrintWriter output;
   @NotNull private final StringBuilder lineSegmentIds;
   private char pathId1;
   private char pathId2;

   PathCoverageFormatter(@NotNull PrintWriter output)
   {
      this.output = output;
      lineSegmentIds = new StringBuilder(100);
   }

   void writeInformationForEachPath(@NotNull List<Path> paths)
   {
      pathId1 = 'A';
      pathId2 = '\0';

      for (Path path : paths) {
         writeCoverageInfoForIndividualPath(path);

         if (pathId2 == '\0' && pathId1 < 'Z') {
            pathId1++;
         }
         else if (pathId2 == '\0') {
            pathId1 = 'A';
            pathId2 = 'A';
         }
         else if (pathId2 < 'Z') {
            pathId2++;
         }
         else {
            pathId1++;
            pathId2 = 'A';
         }
      }
   }

   private void writeCoverageInfoForIndividualPath(@NotNull Path path)
   {
      int executionCount = path.getExecutionCount();
      String lineSegmentIdsForPath = getIdsForLineSegmentsBelongingToThePath(path);

      output.write("        <span class='");
      output.write(executionCount == 0 ? "uncovered" : "covered");
      output.write("' onclick=\"showPath(this,'");
      output.write(lineSegmentIdsForPath);
      output.write("')\">");
      writePathId();
      output.write(": ");
      output.print(executionCount);
      output.println("</span>");
   }

   @NotNull private String getIdsForLineSegmentsBelongingToThePath(@NotNull Path path)
   {
      lineSegmentIds.setLength(0);

      int previousLine = 0;
      int previousSegment = 0;

      for (Node node : path.getNodes()) {
         int line = node.line;
         int segment = node.getSegment();

         if (line > previousLine) {
            appendSegmentId(line, segment);
            previousLine = line;
         }
         else if (segment > previousSegment) {
            appendSegmentId(line, segment);
         }

         previousSegment = segment;
      }

      return lineSegmentIds.toString();
   }

   private void appendSegmentId(int line, int segment)
   {
      if (lineSegmentIds.length() > 0) {
         lineSegmentIds.append(' ');
      }
      
      lineSegmentIds.append('l').append(line).append('s').append(segment);
   }

   private void writePathId()
   {
      output.write(pathId1);

      if (pathId2 != '\0') {
         output.write(pathId2);
      }
   }
}

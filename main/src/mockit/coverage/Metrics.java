/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage;

import javax.annotation.*;

public enum Metrics
{
   LineCoverage(
      "Line", "Line segments",
      "Measures how much of the executable production code was exercised by tests.\r\n" +
      "An executable line of code contains one or more executable segments, separated by branching points (if..else instructions, " +
      "logical operators, etc.).\r\n" +
      "Percentages are calculated as 100*NE/NS, where NS is the number of segments, NE the number of executed segments."),

   DataCoverage(
      "Data", "Fields",
      "Measures how many of the instance and static non-final fields were fully exercised by the test run.\r\n" +
      "To be fully exercised, a field must have the last value assigned to it read by at least one test.\r\n" +
      "Percentages are calculated as 100*NFE/NF, where NF is the number of non-final fields and NFE the number of fully exercised fields.");

   @Nonnull private final String name;
   @Nonnull public final String itemName;
   @Nonnull public final String htmlDescription;

   Metrics(@Nonnull String name, @Nonnull String itemName, @Nonnull String htmlDescription) {
      this.name = name;
      this.itemName = itemName;
      this.htmlDescription = htmlDescription;
   }

   @Override public String toString() { return name; }

   public interface Action { void perform(@Nonnull Metrics metric); }

   public static void performAction(@Nonnull Action action) {
      for (Metrics metric : values()) {
         action.perform(metric);
      }
   }
}

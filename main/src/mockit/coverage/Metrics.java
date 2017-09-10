/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage;

import javax.annotation.*;

public enum Metrics
{
   LineCoverage(
      "Line", "Line segments",
      "Measures how much of the executable production code was exercised by tests.\r\n" +
      "An executable line of code contains one or more executable segments.\r\n" +
      "The percentages are calculated as 100*NE/NS, where NS is the number of segments and NE the number of " +
      "executed segments.",
      isActive("line")),

   PathCoverage(
      "Path", "Paths",
      "Measures how many of the possible execution paths through method/constructor bodies were actually " +
      "executed by tests.\r\n" +
      "The percentages are calculated as 100*NPE/NP, where NP is the number of possible paths and NPE the " +
      "number of fully executed paths.",
      isActive("path")),

   DataCoverage(
      "Data", "Fields",
      "Measures how many of the instance and static non-final fields were fully exercised by the test run.\r\n" +
      "To be fully exercised, a field must have the last value assigned to it read by at least one test.\r\n" +
      "The percentages are calculated as 100*NFE/NF, where NF is the number of non-final fields and NFE the " +
      "number of fully exercised fields.",
      isActive("data"));

   private static boolean isActive(@Nonnull String name)
   {
      String metrics = Configuration.getProperty("metrics", "line");
      boolean all = "all".equals(metrics);
      return all || metrics.contains(name);
   }

   @Nonnull private final String name;
   @Nonnull public final String itemName;
   @Nonnull public final String htmlDescription;
   public final boolean active;

   Metrics(@Nonnull String name, @Nonnull String itemName, @Nonnull String htmlDescription, boolean active)
   {
      this.name = name;
      this.itemName = itemName;
      this.htmlDescription = htmlDescription;
      this.active = active;
   }

   @Override public String toString() { return name; }

   public interface Action { void perform(@Nonnull Metrics metric); }

   public static void performAction(@Nonnull Action action)
   {
      for (Metrics metric : values()) {
         if (metric.active) {
            action.perform(metric);
         }
      }
   }
}

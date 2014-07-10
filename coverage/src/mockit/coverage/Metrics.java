/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage;

import org.jetbrains.annotations.*;

import mockit.coverage.standalone.*;

public enum Metrics
{
   LineCoverage
   {
      @Override public boolean isActive() { return isActive("line"); }
      @Override public String toString() { return "Line"; }
      @Override @NotNull public String itemName() { return "Line segments"; }

      @Override
      @NotNull public String htmlDescription()
      {
         return
            "Measures how much of the executable production code was exercised by tests.\r\n" +
            "An executable line of code contains one or more executable segments.\r\n" +
            "The percentages are calculated as 100*NE/NS, where NS is the number of segments and NE the number of " +
            "executed segments.";
      }
   },

   PathCoverage
   {
      @Override public boolean isActive() { return isActive("path"); }
      @Override public String toString() { return "Path"; }
      @Override @NotNull public String itemName() { return "Paths"; }

      @Override
      @NotNull public String htmlDescription()
      {
         return
            "Measures how many of the possible execution paths through method/constructor bodies were actually " +
            "executed by tests.\r\n" +
            "The percentages are calculated as 100*NPE/NP, where NP is the number of possible paths and NPE the " +
            "number of fully executed paths.";
      }
   },

   DataCoverage
   {
      @Override public boolean isActive()
      {
         return Startup.isTestRun() && Startup.isJMockitAvailable() && isActive("data");
      }

      @Override public String toString() { return "Data"; }
      @Override @NotNull public String itemName() { return "Fields"; }

      @Override
      @NotNull public String htmlDescription()
      {
         return
            "Measures how many of the instance and static non-final fields were fully exercised by the test run.\r\n" +
            "To be fully exercised, a field must have the last value assigned to it read by at least one test.\r\n" +
            "The percentages are calculated as 100*NFE/NF, where NF is the number of non-final fields and NFE the " +
            "number of fully exercised fields.";
      }
   };

   public abstract boolean isActive();
   @NotNull public abstract String itemName();
   @NotNull public abstract String htmlDescription();

   final boolean isActive(@NotNull String name)
   {
      String metrics = Configuration.getProperty("metrics", "line");
      boolean all = "all".equals(metrics);
      return all || metrics.contains(name);
   }

   public interface Action { void perform(@NotNull Metrics metric); }

   public static void performAction(@NotNull Action action)
   {
      for (Metrics metric : values()) {
         if (metric.isActive()) {
            action.perform(metric);
         }
      }
   }
}

/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.testRedundancy;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.*;
import javax.annotation.*;

import mockit.coverage.*;

public final class TestCoverage
{
   @Nullable public static final TestCoverage INSTANCE;

   static {
      INSTANCE = "true".equals(Configuration.getProperty("redundancy", "true")) ? new TestCoverage() : null;
   }

   @Nonnull private final Map<Method, Integer> testsToItemsCovered = new LinkedHashMap<Method, Integer>();
   @Nullable private Method currentTestMethod;

   private TestCoverage() {}

   public void setCurrentTestMethod(@Nullable Method testMethod) {
      if (testMethod != null) {
         testsToItemsCovered.put(testMethod, 0);
      }

      currentTestMethod = testMethod;
   }

   public void recordNewItemCoveredByTestIfApplicable(@Nonnegative int previousExecutionCount) {
      if (previousExecutionCount == 0 && currentTestMethod != null) {
         Integer itemsCoveredByTest = testsToItemsCovered.get(currentTestMethod);
         testsToItemsCovered.put(currentTestMethod, itemsCoveredByTest == null ? 1 : itemsCoveredByTest + 1);
      }
   }

   @Nonnull
   public List<Method> getRedundantTests() {
      List<Method> redundantTests = new ArrayList<Method>();

      for (Entry<Method, Integer> testAndItemsCovered : testsToItemsCovered.entrySet()) {
         Method testMethod = testAndItemsCovered.getKey();
         Integer itemsCovered = testAndItemsCovered.getValue();

         if (itemsCovered == 0) {
            redundantTests.add(testMethod);
         }
      }

      return redundantTests;
   }
}

/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.dataItems;

import java.util.*;

import org.jetbrains.annotations.*;

import mockit.internal.state.*;

public final class InstanceFieldData extends FieldData
{
   private static final long serialVersionUID = 6991762113575259754L;

   @NotNull private final transient Map<Integer, List<Integer>> testIdsToAssignments =
      new HashMap<Integer, List<Integer>>();

   void registerAssignment(@NotNull Object instance)
   {
      List<Integer> dataForRunningTest = getDataForRunningTest();
      Integer instanceId = System.identityHashCode(instance);

      if (!dataForRunningTest.contains(instanceId)) {
         dataForRunningTest.add(instanceId);
      }

      writeCount++;
   }

   void registerRead(@NotNull Object instance)
   {
      List<Integer> dataForRunningTest = getDataForRunningTest();
      Integer instanceId = System.identityHashCode(instance);

      dataForRunningTest.remove(instanceId);
      readCount++;
   }

   @NotNull private List<Integer> getDataForRunningTest()
   {
      int testId = TestRun.getTestId();
      List<Integer> fieldData = testIdsToAssignments.get(testId);

      if (fieldData == null) {
         fieldData = new LinkedList<Integer>();
         testIdsToAssignments.put(testId, fieldData);
      }

      return fieldData;
   }

   @Override
   void markAsCoveredIfNoUnreadValuesAreLeft()
   {
      for (List<Integer> unreadInstances : testIdsToAssignments.values()) {
         if (unreadInstances.isEmpty()) {
            covered = true;
            break;
         }
      }
   }

   @NotNull public List<Integer> getOwnerInstancesWithUnreadAssignments()
   {
      if (isCovered()) {
         return Collections.emptyList();
      }

      Collection<List<Integer>> assignments = testIdsToAssignments.values();
      return assignments.iterator().next();
   }
}

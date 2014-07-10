/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.dataItems;

import java.io.*;
import java.util.*;
import java.util.Map.*;

import org.jetbrains.annotations.*;

import mockit.coverage.*;
import mockit.coverage.data.*;

public final class PerFileDataCoverage implements PerFileCoverage
{
   private static final long serialVersionUID = -4561686103982673490L;

   @NotNull public final List<String> allFields = new ArrayList<String>(2);
   @NotNull public final Map<String, StaticFieldData> staticFieldsData = new LinkedHashMap<String, StaticFieldData>();
   @NotNull public final Map<String, InstanceFieldData> instanceFieldsData = new LinkedHashMap<String, InstanceFieldData>();

   private transient int coveredDataItems = -1;

   private void readObject(@NotNull ObjectInputStream in) throws IOException, ClassNotFoundException
   {
      coveredDataItems = -1;
      in.defaultReadObject();
   }

   public void addField(@NotNull String className, @NotNull String fieldName, boolean isStatic)
   {
      String classAndField = className + '.' + fieldName;
      allFields.add(classAndField);

      if (isStatic) {
         staticFieldsData.put(classAndField, new StaticFieldData());
      }
      else {
         instanceFieldsData.put(classAndField, new InstanceFieldData());
      }
   }

   public boolean isFieldWithCoverageData(@NotNull String classAndFieldNames)
   {
      return
         instanceFieldsData.containsKey(classAndFieldNames) ||
         staticFieldsData.containsKey(classAndFieldNames);
   }

   public synchronized void registerAssignmentToStaticField(@NotNull String classAndFieldNames)
   {
      StaticFieldData staticData = getStaticFieldData(classAndFieldNames);

      if (staticData != null) {
         staticData.registerAssignment();
      }
   }

   @Nullable public StaticFieldData getStaticFieldData(@NotNull String classAndFieldNames)
   {
      return staticFieldsData.get(classAndFieldNames);
   }

   public synchronized void registerReadOfStaticField(@NotNull String classAndFieldNames)
   {
      StaticFieldData staticData = getStaticFieldData(classAndFieldNames);

      if (staticData != null) {
         staticData.registerRead();
      }
   }

   public synchronized void registerAssignmentToInstanceField(
      @NotNull Object instance, @NotNull String classAndFieldNames)
   {
      InstanceFieldData instanceData = getInstanceFieldData(classAndFieldNames);

      if (instanceData != null) {
         instanceData.registerAssignment(instance);
      }
   }

   @Nullable public InstanceFieldData getInstanceFieldData(@NotNull String classAndFieldNames)
   {
      return instanceFieldsData.get(classAndFieldNames);
   }

   public synchronized void registerReadOfInstanceField(@NotNull Object instance, @NotNull String classAndFieldNames)
   {
      InstanceFieldData instanceData = getInstanceFieldData(classAndFieldNames);

      if (instanceData != null) {
         instanceData.registerRead(instance);
      }
   }

   public boolean hasFields() { return !allFields.isEmpty(); }

   public boolean isCovered(@NotNull String classAndFieldNames)
   {
      InstanceFieldData instanceData = getInstanceFieldData(classAndFieldNames);

      if (instanceData != null && instanceData.isCovered()) {
         return true;
      }

      StaticFieldData staticData = getStaticFieldData(classAndFieldNames);

      return staticData != null && staticData.isCovered();
   }

   @Override
   public int getTotalItems()
   {
      return staticFieldsData.size() + instanceFieldsData.size();
   }

   @Override
   public int getCoveredItems()
   {
      if (coveredDataItems >= 0) {
         return coveredDataItems;
      }

      coveredDataItems = 0;

      for (StaticFieldData staticData : staticFieldsData.values()) {
         if (staticData.isCovered()) {
            coveredDataItems++;
         }
      }

      for (InstanceFieldData instanceData : instanceFieldsData.values()) {
         if (instanceData.isCovered()) {
            coveredDataItems++;
         }
      }

      return coveredDataItems;
   }

   @Override
   public int getCoveragePercentage()
   {
      int totalFields = getTotalItems();

      if (totalFields == 0) {
         return -1;
      }

      return CoveragePercentage.calculate(getCoveredItems(), totalFields);
   }

   public void mergeInformation(@NotNull PerFileDataCoverage previousInfo)
   {
      addInfoFromPreviousTestRun(staticFieldsData, previousInfo.staticFieldsData);
      addFieldsFromPreviousTestRunIfAbsent(staticFieldsData, previousInfo.staticFieldsData);

      addInfoFromPreviousTestRun(instanceFieldsData, previousInfo.instanceFieldsData);
      addFieldsFromPreviousTestRunIfAbsent(instanceFieldsData, previousInfo.instanceFieldsData);
   }

   private <FI extends FieldData> void addInfoFromPreviousTestRun(
      @NotNull Map<String, FI> currentInfo, @NotNull Map<String, FI> previousInfo)
   {
      for (Entry<String, FI> nameAndInfo : currentInfo.entrySet()) {
         String fieldName = nameAndInfo.getKey();
         FieldData previousFieldInfo = previousInfo.get(fieldName);

         if (previousFieldInfo != null) {
            FieldData fieldInfo = nameAndInfo.getValue();
            fieldInfo.addCountsFromPreviousTestRun(previousFieldInfo);
         }
      }
   }

   private <FI extends FieldData> void addFieldsFromPreviousTestRunIfAbsent(
      @NotNull Map<String, FI> currentInfo, @NotNull Map<String, FI> previousInfo)
   {
      for (Entry<String, FI> nameAndInfo : previousInfo.entrySet()) {
         String fieldName = nameAndInfo.getKey();

         if (!currentInfo.containsKey(fieldName)) {
            currentInfo.put(fieldName, previousInfo.get(fieldName));
         }
      }
   }
}

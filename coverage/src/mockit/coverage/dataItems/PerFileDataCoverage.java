/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.dataItems;

import java.io.*;
import java.util.*;
import java.util.Map.*;
import javax.annotation.*;

import mockit.coverage.*;
import mockit.coverage.data.*;

public final class PerFileDataCoverage implements PerFileCoverage
{
   private static final long serialVersionUID = -4561686103982673490L;

   @Nonnull public final List<String> allFields = new ArrayList<String>(2);
   @Nonnull public final Map<String, StaticFieldData> staticFieldsData = new LinkedHashMap<String, StaticFieldData>();
   @Nonnull public final Map<String, InstanceFieldData> instanceFieldsData = new LinkedHashMap<String, InstanceFieldData>();

   private transient int coveredDataItems = -1;

   private void readObject(@Nonnull ObjectInputStream in) throws IOException, ClassNotFoundException
   {
      coveredDataItems = -1;
      in.defaultReadObject();
   }

   public void addField(@Nonnull String className, @Nonnull String fieldName, boolean isStatic)
   {
      String classAndField = className + '.' + fieldName;

      if (!allFields.contains(classAndField)) {
         allFields.add(classAndField);
      }

      if (isStatic) {
         staticFieldsData.put(classAndField, new StaticFieldData());
      }
      else {
         instanceFieldsData.put(classAndField, new InstanceFieldData());
      }
   }

   public boolean isFieldWithCoverageData(@Nonnull String classAndFieldNames)
   {
      return
         instanceFieldsData.containsKey(classAndFieldNames) ||
         staticFieldsData.containsKey(classAndFieldNames);
   }

   public void registerAssignmentToStaticField(@Nonnull String classAndFieldNames)
   {
      StaticFieldData staticData = getStaticFieldData(classAndFieldNames);

      if (staticData != null) {
         staticData.registerAssignment();
      }
   }

   @Nullable public StaticFieldData getStaticFieldData(@Nonnull String classAndFieldNames)
   {
      return staticFieldsData.get(classAndFieldNames);
   }

   public void registerReadOfStaticField(@Nonnull String classAndFieldNames)
   {
      StaticFieldData staticData = getStaticFieldData(classAndFieldNames);

      if (staticData != null) {
         staticData.registerRead();
      }
   }

   public void registerAssignmentToInstanceField(@Nonnull Object instance, @Nonnull String classAndFieldNames)
   {
      InstanceFieldData instanceData = getInstanceFieldData(classAndFieldNames);

      if (instanceData != null) {
         instanceData.registerAssignment(instance);
      }
   }

   @Nullable public InstanceFieldData getInstanceFieldData(@Nonnull String classAndFieldNames)
   {
      return instanceFieldsData.get(classAndFieldNames);
   }

   public void registerReadOfInstanceField(@Nonnull Object instance, @Nonnull String classAndFieldNames)
   {
      InstanceFieldData instanceData = getInstanceFieldData(classAndFieldNames);

      if (instanceData != null) {
         instanceData.registerRead(instance);
      }
   }

   public boolean hasFields() { return !allFields.isEmpty(); }

   public boolean isCovered(@Nonnull String classAndFieldNames)
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

   public void mergeInformation(@Nonnull PerFileDataCoverage previousInfo)
   {
      addInfoFromPreviousTestRun(staticFieldsData, previousInfo.staticFieldsData);
      addFieldsFromPreviousTestRunIfAbsent(staticFieldsData, previousInfo.staticFieldsData);

      addInfoFromPreviousTestRun(instanceFieldsData, previousInfo.instanceFieldsData);
      addFieldsFromPreviousTestRunIfAbsent(instanceFieldsData, previousInfo.instanceFieldsData);
   }

   private static <FI extends FieldData> void addInfoFromPreviousTestRun(
      @Nonnull Map<String, FI> currentInfo, @Nonnull Map<String, FI> previousInfo)
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

   private static <FI extends FieldData> void addFieldsFromPreviousTestRunIfAbsent(
      @Nonnull Map<String, FI> currentInfo, @Nonnull Map<String, FI> previousInfo)
   {
      for (Entry<String, FI> nameAndInfo : previousInfo.entrySet()) {
         String fieldName = nameAndInfo.getKey();

         if (!currentInfo.containsKey(fieldName)) {
            currentInfo.put(fieldName, previousInfo.get(fieldName));
         }
      }
   }
}

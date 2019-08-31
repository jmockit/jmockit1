package integrationTests;

import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;
import static java.lang.reflect.Modifier.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.coverage.*;
import mockit.coverage.data.*;
import mockit.coverage.dataItems.*;
import mockit.coverage.lines.*;

@SuppressWarnings("JUnitTestCaseWithNoTests")
public class CoverageTest
{
   @Nullable protected static FileCoverageData fileData;
   @Nullable private static String testedClassSimpleName;

   @Before
   public final void findCoverageData() throws Exception {
      Field testedField = getClass().getDeclaredField("tested");
      Class<?> testedClass = testedField.getType();

      if (testedClass != Object.class) {
         findFileDate(testedClass);
         setTestedFieldToNewInstanceIfApplicable(testedField);
      }
   }

   private void findFileDate(@Nonnull Class<?> testedClass) {
      testedClassSimpleName = testedClass.getSimpleName();

      String classFilePath = testedClass.getName().replace('.', '/') + ".java";
      Map<String, FileCoverageData> data = CoverageData.instance().getFileToFileData();
      fileData = data.get(classFilePath);

      assertNotNull("FileCoverageData not found for " + classFilePath, fileData);
   }

   private void setTestedFieldToNewInstanceIfApplicable(@Nonnull Field testedField) throws Exception {
      Class<?> testedClass = testedField.getType();

      if (!testedClass.isEnum() && !isAbstract(testedClass.getModifiers()) && !isFinal(testedField.getModifiers())) {
         testedField.setAccessible(true);

         if (testedField.get(this) == null) {
            //noinspection ClassNewInstance
            Object newTestedInstance = testedClass.newInstance();

            testedField.set(this, newTestedInstance);
         }
      }
   }

   @Nonnull
   private FileCoverageData fileData() {
      if (fileData == null) {
         Object testedInstance;

         try {
            Field testedField = getClass().getDeclaredField("tested");
            testedInstance = testedField.get(this);
         }
         catch (NoSuchFieldException | IllegalAccessException e) { throw new RuntimeException(e); }

         Class<?> testedClass = testedInstance.getClass();
         findFileDate(testedClass);
      }

      return fileData;
   }

   // Line Coverage assertions ////////////////////////////////////////////////////////////////////////////////////////////////////////////

   protected final void assertLines(@Nonnegative int startingLine, @Nonnegative int endingLine, @Nonnegative int expectedLinesExecuted) {
      PerFileLineCoverage lineCoverageInfo = fileData().lineCoverageInfo;
      int lineCount = lineCoverageInfo.getLineCount();
      assertTrue("Starting line not found", lineCount >= startingLine);
      assertTrue("Ending line not found", lineCount >= endingLine);

      int linesExecuted = 0;

      for (int line = startingLine; line <= endingLine; line++) {
         if (lineCoverageInfo.getExecutionCount(line) > 0) {
            linesExecuted++;
         }
      }

      assertEquals("Unexpected number of lines executed:", expectedLinesExecuted, linesExecuted);
   }

   protected final void assertLine(
      @Nonnegative int line, @Nonnegative int expectedSegments, @Nonnegative int expectedCoveredSegments, int... expectedExecutionCounts
   ) {
      PerFileLineCoverage info = fileData().lineCoverageInfo;
      LineCoverageData lineData = info.getLineData(line);

      assertEquals("Segments:", expectedSegments, info.getNumberOfSegments(line));
      assertEquals("Covered segments:", expectedCoveredSegments, lineData.getNumberOfCoveredSegments());
      assertEquals("Execution count:", expectedExecutionCounts[0], info.getExecutionCount(line));

      for (int i = 1; i < expectedExecutionCounts.length; i++) {
         BranchCoverageData segmentData = lineData.getBranchData(i - 1);

         int executionCount = segmentData.getExecutionCount();
         assertEquals(
            "Execution count for line " + line + ", segment " + i + ':', expectedExecutionCounts[i], executionCount);

         List<CallPoint> callPoints = segmentData.getCallPoints();

         if (callPoints != null) {
            int callPointCount = 0;

            for (CallPoint callPoint : callPoints) {
               callPointCount++;
               callPointCount += callPoint.getRepetitionCount();
            }

            assertEquals("Missing call points for line " + line + ", segment " + i, executionCount, callPointCount);
         }
      }
   }

   protected final void assertBranchingPoints(
      @Nonnegative int line, @Nonnegative int expectedSourcesAndTargets, @Nonnegative int expectedCoveredSourcesAndTargets
   ) {
      PerFileLineCoverage lineCoverageInfo = fileData().lineCoverageInfo;
      LineCoverageData lineData = lineCoverageInfo.getLineData(line);

      int sourcesAndTargets = lineCoverageInfo.getNumberOfBranchingSourcesAndTargets(line);
      assertEquals("Sources and targets:", expectedSourcesAndTargets, sourcesAndTargets);

      int coveredSourcesAndTargets = lineData.getNumberOfCoveredBranchingSourcesAndTargets();
      assertEquals("Covered sources and targets:", expectedCoveredSourcesAndTargets, coveredSourcesAndTargets);
   }

   // Data Coverage assertions ////////////////////////////////////////////////////////////////////////////////////////////////////////////

   protected final void assertFieldIgnored(@Nonnull String fieldName) {
      String fieldId = testedClassSimpleName + '.' + fieldName;
      PerFileDataCoverage info = fileData().dataCoverageInfo;
      assertFalse("Field " + fieldName + " should not have static coverage data", info.staticFieldsData.containsKey(fieldId));
      assertFalse("Field " + fieldName + " should not have instance coverage data", info.instanceFieldsData.containsKey(fieldId));
   }

   protected static void assertStaticFieldCovered(@Nonnull String fieldName) {
      assertTrue("Static field " + fieldName + " should be covered", isStaticFieldCovered(fieldName));
   }

   private static boolean isStaticFieldCovered(@Nonnull String fieldName) {
      String classAndFieldNames = testedClassSimpleName + '.' + fieldName;
      StaticFieldData staticFieldData = fileData.dataCoverageInfo.staticFieldsData.get(classAndFieldNames);

      return staticFieldData.isCovered();
   }

   protected static void assertStaticFieldUncovered(@Nonnull String fieldName) {
      assertFalse("Static field " + fieldName + " should not be covered", isStaticFieldCovered(fieldName));
   }

   protected static void assertInstanceFieldCovered(@Nonnull String fieldName) {
      assertTrue("Instance field " + fieldName + " should be covered", isInstanceFieldCovered(fieldName));
   }

   private static boolean isInstanceFieldCovered(@Nonnull String fieldName) {
      return getInstanceFieldData(fieldName).isCovered();
   }

   private static InstanceFieldData getInstanceFieldData(@Nonnull String fieldName) {
      String classAndFieldNames = testedClassSimpleName + '.' + fieldName;
      return fileData.dataCoverageInfo.instanceFieldsData.get(classAndFieldNames);
   }

   protected static void assertInstanceFieldUncovered(@Nonnull String fieldName) {
      assertFalse("Instance field " + fieldName + " should not be covered", isInstanceFieldCovered(fieldName));
   }

   protected static void assertInstanceFieldUncovered(@Nonnull String fieldName, @Nonnull Object... uncoveredInstances) {
      String msg = "Instance field " + fieldName + " should not be covered";
      InstanceFieldData fieldData = getInstanceFieldData(fieldName);
      List<Integer> ownerInstances = fieldData.getOwnerInstancesWithUnreadAssignments();

      assertEquals(msg, uncoveredInstances.length, ownerInstances.size());

      for (Object uncoveredInstance : uncoveredInstances) {
         Integer instanceId = System.identityHashCode(uncoveredInstance);
         assertTrue(msg, ownerInstances.contains(instanceId));
      }
   }

   protected static void verifyDataCoverage(
      @Nonnegative int expectedItems, @Nonnegative int expectedCoveredItems, @Nonnegative int expectedCoverage
   ) {
      PerFileDataCoverage info = fileData.dataCoverageInfo;
      assertEquals("Total data items:", expectedItems, info.getTotalItems());
      assertEquals("Covered data items:", expectedCoveredItems, info.getCoveredItems());
      assertEquals("Data coverage:", expectedCoverage, info.getCoveragePercentage());
   }
}
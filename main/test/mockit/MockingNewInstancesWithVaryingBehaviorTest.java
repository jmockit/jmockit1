/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.text.*;
import java.util.*;

import static org.junit.Assert.*;
import org.junit.*;

public final class MockingNewInstancesWithVaryingBehaviorTest
{
   static final String DATE_FORMAT = "yyyy-MM-dd";
   static final String FORMATTED_DATE = "2012-02-28";

   static final String TIME_FORMAT = "HH";
   static final String FORMATTED_TIME = "13";

   void exerciseAndVerifyTestedCode()
   {
      Date now = new Date();

      String hour = new SimpleDateFormat(TIME_FORMAT).format(now);
      assertEquals(FORMATTED_TIME, hour);

      String date = new SimpleDateFormat(DATE_FORMAT).format(now);
      assertEquals(FORMATTED_DATE, date);
   }

   /// Tests using the Faking API /////////////////////////////////////////////////////////////////////////////////////

   DateFormat dateFormat;
   DateFormat hourFormat;

   @Test
   public void usingFakesWithInvocationParameter()
   {
      new MockUp<SimpleDateFormat>() {
         @Mock
         void $init(Invocation inv, String pattern)
         {
            DateFormat dt = inv.getInvokedInstance();
            if (DATE_FORMAT.equals(pattern)) dateFormat = dt;
            else if (TIME_FORMAT.equals(pattern)) hourFormat = dt;
         }
      };

      new MockUp<DateFormat>() {
         @Mock
         String format(Invocation inv, Date d)
         {
            assertNotNull(d);
            DateFormat dt = inv.getInvokedInstance();
            if (dt == dateFormat) return FORMATTED_DATE;
            else if (dt == hourFormat) return FORMATTED_TIME;
            else return null;
         }
      };

      exerciseAndVerifyTestedCode();
   }

   /// Tests using the Mocking API ////////////////////////////////////////////////////////////////////////////////////

   @Test // not too complex, but inelegant
   public void usingPartialMockingAndDelegate()
   {
      final SimpleDateFormat sdf = new SimpleDateFormat();

      new Expectations(SimpleDateFormat.class) {{
         sdf.format((Date) any);
         result = new Delegate() {
            @Mock String format(Invocation inv)
            {
               String pattern = inv.<SimpleDateFormat>getInvokedInstance().toPattern();
               if (DATE_FORMAT.equals(pattern)) return FORMATTED_DATE;
               else if (TIME_FORMAT.equals(pattern)) return FORMATTED_TIME;
               else return null;
            }
         };
      }};

      exerciseAndVerifyTestedCode();
   }

   @Test // nice
   public void usingReplacementInstances(@Mocked final SimpleDateFormat dateFmt, @Mocked final SimpleDateFormat hourFmt)
   {
      new Expectations() {{
         new SimpleDateFormat(DATE_FORMAT); result = dateFmt;
         dateFmt.format((Date) any); result = FORMATTED_DATE;

         new SimpleDateFormat(TIME_FORMAT); result = hourFmt;
         hourFmt.format((Date) any); result = FORMATTED_TIME;
      }};

      exerciseAndVerifyTestedCode();
   }

   @Test // nicer
   public void usingInstantiationRecording(@Mocked SimpleDateFormat anyDateFormat)
   {
      new Expectations() {{
         SimpleDateFormat dateFmt = new SimpleDateFormat(DATE_FORMAT);
         dateFmt.format((Date) any); result = FORMATTED_DATE;

         SimpleDateFormat hourFmt = new SimpleDateFormat(TIME_FORMAT);
         hourFmt.format((Date) any); result = FORMATTED_TIME;
      }};

      exerciseAndVerifyTestedCode();
   }

   static class Collaborator
   {
      final int value;
      Collaborator() { value = -1; }
      Collaborator(int value) { this.value = value; }
      Collaborator(String value) { this.value = Integer.parseInt(value); }
      int getValue() { return value; }
      boolean isPositive() { return value > 0; }
      String doSomething(String s) { return s + ": " + value; }
      int doSomething(int i) { return i; }
   }

   @Test
   public void matchMethodCallsOnInstancesCreatedWithConstructorMatchingRecordedOne(
      @Mocked final Collaborator mock, @Mocked final Collaborator mock5, @Mocked final Collaborator mock6)
   {
      new Expectations() {{
         new Collaborator(5); result = mock5;
         mock5.getValue(); result = 123; times = 2;

         new Collaborator(); result = mock; times = 1;

         new Collaborator(6); result = mock6;
      }};

      assertEquals(0, new Collaborator().getValue());

      Collaborator newCol1 = new Collaborator(5);
      assertEquals(123, newCol1.getValue());

      Collaborator newCol2 = new Collaborator(6);
      assertEquals(0, newCol2.getValue());
      assertFalse(newCol2.isPositive());

      Collaborator newCol3 = new Collaborator(5);
      assertEquals(123, newCol3.getValue());
      assertFalse(newCol3.isPositive());

      new Verifications() {{
         new Collaborator(anyInt); times = 3;
         mock.getValue(); times = 1;
         mock6.getValue(); times = 1;
         mock5.isPositive(); times = 1;
         mock6.isPositive(); times = 1;
      }};
   }

   @Test
   public void mockInstancesMatchingRecordedConstructorInvocationsToHaveSameBehaviorAsOtherUnmockedInstances()
   {
      final Collaborator col1 = new Collaborator(1);
      final Collaborator col2 = new Collaborator(-2);

      new Expectations(Collaborator.class) {{
         new Collaborator(3); result = col1;
         new Collaborator(5); result = col2;
         col1.doSomething("recorded"); result = "mocked";
      }};

      Collaborator newCol1 = new Collaborator(-10);
      assertEquals(-10, newCol1.getValue());
      assertEquals("not mocked: -10", newCol1.doSomething("not mocked"));
      assertEquals("recorded: -10", newCol1.doSomething("recorded"));

      Collaborator newCol2 = new Collaborator(3);
      assertEquals(1, newCol2.getValue());
      assertEquals("mocked", newCol2.doSomething("recorded"));
      assertEquals("not recorded: 1", newCol2.doSomething("not recorded"));

      Collaborator newCol3 = new Collaborator(5);
      assertEquals(-2, newCol3.getValue());
      assertFalse(newCol3.isPositive());
      assertEquals("null: -2", newCol3.doSomething(null));

      Collaborator newCol4 = new Collaborator(10);
      assertEquals(10, newCol4.getValue());
      assertTrue(newCol4.isPositive());

      Collaborator newCol5 = new Collaborator(3);
      assertEquals(1, newCol5.getValue());
      assertTrue(newCol5.isPositive());
      assertEquals("mocked", newCol5.doSomething("recorded"));
      assertEquals("test: 1", newCol5.doSomething("test"));

      new Verifications() {{
         col1.getValue(); times = 2;
         col1.isPositive(); times = 1;

         col2.getValue(); times = 1;
         col2.isPositive(); times = 1;

         col1.doSomething(anyString); times = 4;
         col2.doSomething(anyString); times = 1;
      }};
   }

   @Test
   public void recordDifferentResultsForInstancesCreatedWithDifferentConstructors(@Mocked final Collaborator anyCol)
   {
      new Expectations() {{
         anyCol.getValue(); result = 1;

         Collaborator col2 = new Collaborator(anyInt);
         col2.getValue(); result = 2;
      }};

      int valueFromRecordedConstructor = new Collaborator(10).getValue();
      int valueFromAnyOtherConstructor = new Collaborator().getValue();

      assertEquals(2, valueFromRecordedConstructor);
      assertEquals(1, valueFromAnyOtherConstructor);
   }
}

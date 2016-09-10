/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.*;

import org.junit.*;
import org.junit.rules.*;

import mockit.internal.*;

public final class VerificationsWithSomeArgumentMatchersTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   @SuppressWarnings("UnusedParameters")
   static class Collaborator
   {
      void setValue(int value) {}
      void setValue(double value) {}
      void setValue(float value) {}

      void setValues(long value1, Byte value2, double value3, Short value4) {}
      boolean booleanValues(long value1, byte value2, double value3, short value4) { return true; }
      static void staticSetValues(long value1, byte value2, double value3, short value4) {}
      static long staticLongValues(long value1, byte value2, double value3, char value4)
      {
         return -2;
      }

      final void simpleOperation(int a, String b, Date c) {}
      long anotherOperation(byte b, long l) { return -1; }

      static void staticVoidMethod(long l, char c, float f) {}
      static boolean staticBooleanMethod(boolean b, String s, int[] array) { return false; }
   }

   @Mocked Collaborator mock;

   @Test
   public void useMatcherOnlyForOneArgument()
   {
      mock.simpleOperation(1, "", null);
      mock.simpleOperation(2, "str", null);
      mock.simpleOperation(1, "", null);
      mock.simpleOperation(12, "arg", new Date());

      mock.anotherOperation((byte) 0, 5);
      mock.anotherOperation((byte) 3, 5);

      Collaborator.staticVoidMethod(34L, '8', 5.0F);
      Collaborator.staticBooleanMethod(true, "start-end", null);

      new VerificationsInOrder() {{
         mock.simpleOperation(withEqual(1), "", null);
         mock.simpleOperation(withNotEqual(1), null, (Date) withNull());
         mock.simpleOperation(1, withNotEqual("arg"), null);
         mock.simpleOperation(12, "arg", (Date) withNotNull());

         mock.anotherOperation((byte) 0, anyLong);
         mock.anotherOperation(anyByte, 5);

         Collaborator.staticVoidMethod(34L, anyChar, 5.0F);
         Collaborator.staticBooleanMethod(true, withSuffix("end"), null);
      }};
   }

   @Test
   public void useMatcherOnlyForFirstArgumentWithUnexpectedReplayValue()
   {
      thrown.expect(MissingInvocation.class);

      mock.simpleOperation(2, "", null);

      new Verifications() {{
         mock.simpleOperation(withEqual(1), "", null);
      }};
   }

   @Test
   public void useMatcherOnlyForSecondArgumentWithUnexpectedReplayValue()
   {
      thrown.expect(MissingInvocation.class);

      mock.simpleOperation(1, "Xyz", null);

      new Verifications() {{
         mock.simpleOperation(1, withPrefix("arg"), null);
      }};
   }

   @Test
   public void useMatcherOnlyForLastArgumentWithUnexpectedReplayValue()
   {
      thrown.expect(MissingInvocation.class);

      mock.simpleOperation(12, "arg", null);

      new Verifications() {{
         mock.simpleOperation(12, "arg", (Date) withNotNull());
      }};
   }

   @Test
   public void useMatchersForParametersOfAllSizes()
   {
      mock.setValues(123L, (byte) 5, 6.4, (short) 41);
      mock.booleanValues(12L, (byte) 4, 6.1, (short) 14);
      Collaborator.staticSetValues(2L, (byte) 4, 6.1, (short) 3);
      Collaborator.staticLongValues(12L, (byte) -7, 6.1, 'F');

      new Verifications() {{
         mock.setValues(123L, anyByte, 6.4, anyShort);
         mock.booleanValues(12L, (byte) 4, withEqual(6.0, 0.1), withEqual((short) 14));
         Collaborator.staticSetValues(withNotEqual(1L), (byte) 4, 6.1, withEqual((short) 3));
         Collaborator.staticLongValues(12L, anyByte, withEqual(6.1), 'F');
      }};
   }

   @Test
   public void useAnyIntField()
   {
      mock.setValue(1);

      new FullVerifications() {{ mock.setValue(anyInt); }};
   }

   @Test
   public void useSeveralAnyFields()
   {
      final Date now = new Date();
      mock.simpleOperation(2, "abc", now);
      mock.simpleOperation(5, "test", null);
      mock.simpleOperation(3, "test2", null);
      mock.simpleOperation(-1, "Xyz", now);
      mock.simpleOperation(1, "", now);

      Collaborator.staticSetValues(2, (byte) 1, 0, (short) 2);
      Collaborator.staticLongValues(23L, (byte) 1, 1.34, 'S');
      Collaborator.staticVoidMethod(45L, 'S', 56.4F);

      new FullVerificationsInOrder() {{
         mock.simpleOperation(anyInt, null, null);
         mock.simpleOperation(anyInt, "test", null);
         mock.simpleOperation(3, "test2", null);
         mock.simpleOperation(-1, null, (Date) any);
         mock.simpleOperation(1, anyString, now);

         Collaborator.staticSetValues(2L, anyByte, 0.0, anyShort);
         Collaborator.staticLongValues(anyLong, (byte) 1, anyDouble, anyChar);
         Collaborator.staticVoidMethod(45L, 'S', anyFloat);
      }};
   }

   @Test
   public void useWithMethodsMixedWithAnyFields()
   {
      Date now = new Date();
      mock.simpleOperation(2, "abc", now);
      mock.simpleOperation(5, "test", null);
      mock.simpleOperation(3, "test2", null);
      mock.simpleOperation(-1, "Xyz", now);
      mock.simpleOperation(1, "", now);

      new Verifications() {{
         mock.simpleOperation(anyInt, null, (Date) any);
         mock.simpleOperation(anyInt, withEqual("test"), null);
         mock.simpleOperation(3, withPrefix("test"), (Date) any);
         mock.simpleOperation(-1, anyString, (Date) any);
         mock.simpleOperation(1, anyString, (Date) withNotNull());
      }};
   }

   public interface Scheduler
   {
      List<String> getAlerts(Object o, int i, boolean b);
   }

   @Test
   public void useMatchersInInvocationsToInterfaceMethods(@Mocked final Scheduler scheduler)
   {
      scheduler.getAlerts("123", 1, true);
      scheduler.getAlerts(null, 1, false);

      new FullVerifications() {{
         scheduler.getAlerts(any, 1, anyBoolean); times = 2;
      }};

      new Verifications() {{
         scheduler.getAlerts(null, anyInt, anyBoolean); times = 2;
      }};
   }
}
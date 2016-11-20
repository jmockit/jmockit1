/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;
import org.junit.rules.*;

import mockit.internal.*;

public final class VerificationsInOrderTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   @SuppressWarnings("UnusedDeclaration")
   public static class Dependency
   {
      public void setSomething(int value) {}
      public void setSomethingElse(String value) {}
      public void editABunchMoreStuff() {}
      public void notifyBeforeSave() {}
      public void prepare() {}
      public void save() {}
      void doSomething(ClassWithHashCode h) {}
   }

   static final class ClassWithHashCode
   {
      @Override
      public boolean equals(Object obj) { return obj instanceof ClassWithHashCode && this == obj; }

      @Override
      public int hashCode() { return 123; }
   }

   @Mocked Dependency mock;

   void exerciseCodeUnderTest()
   {
      mock.prepare();
      mock.setSomething(123);
      mock.setSomethingElse("anotherValue");
      mock.setSomething(45);
      mock.editABunchMoreStuff();
      mock.notifyBeforeSave();
      mock.save();
   }

   @Test
   public void verifySimpleInvocations()
   {
      exerciseCodeUnderTest();

      new VerificationsInOrder() {{
         mock.prepare();
         mock.setSomething(45);
         mock.editABunchMoreStuff();
      }};
   }

   @Test
   public void verifyUnrecordedInvocationThatShouldHappenButDoesNot()
   {
      thrown.expect(MissingInvocation.class);

      mock.setSomething(1);

      new VerificationsInOrder() {{ mock.notifyBeforeSave(); }};
   }

   @Test
   public void verifyUnrecordedInvocationThatShouldHappenExactlyOnceButDoesNot()
   {
      thrown.expect(MissingInvocation.class);
      thrown.expectMessage("2");

      mock.setSomething(1);

      new VerificationsInOrder() {{ mock.setSomething(2); times = 1; }};
   }

   @Test
   public void verifyRecordedInvocationThatShouldHappenButDoesNot()
   {
      thrown.expect(MissingInvocation.class);

      new Expectations() {{
         mock.setSomething(1);
         mock.notifyBeforeSave();
      }};

      mock.setSomething(1);

      new VerificationsInOrder() {{
         mock.setSomething(1);
         mock.notifyBeforeSave();
      }};
   }

   @Test
   public void verifyAllInvocationsWithSomeOfThemRecorded()
   {
      new Expectations() {{
         mock.prepare();
         mock.editABunchMoreStuff();
      }};

      exerciseCodeUnderTest();

      new VerificationsInOrder() {{
         mock.prepare(); minTimes = 1;
         mock.setSomethingElse(anyString);
         mock.setSomething(anyInt); minTimes = 1; maxTimes = 2;
         mock.editABunchMoreStuff();
         mock.notifyBeforeSave(); maxTimes = 1;
         mock.save(); times = 1;
      }};
   }

   @Test
   public void verifyInvocationsWithExactInvocationCountsHavingRecordedMatchingExpectationWithArgumentMatcher()
   {
      new Expectations() {{ mock.setSomething(anyInt); }};

      mock.setSomething(1);
      mock.setSomething(2);

      new VerificationsInOrder() {{
         mock.setSomething(1); times = 1;
         mock.setSomething(2); times = 1;
      }};
   }

   @Test
   public void verifyInvocationThatIsAllowedToHappenAnyNumberOfTimesAndHappensOnce()
   {
      mock.prepare();
      mock.setSomething(123);
      mock.save();

      new VerificationsInOrder() {{
         mock.prepare();
         mock.setSomething(anyInt);
         mock.save();
      }};
   }

   @Test
   public void verifySimpleInvocationsWhenOutOfOrder()
   {
      thrown.expect(MissingInvocation.class);
      thrown.expectMessage("123");

      mock.setSomething(123);
      mock.prepare();

      new VerificationsInOrder() {{
         mock.prepare();
         mock.setSomething(123);
      }};
   }

   @Test
   public void verifyRepeatingInvocation()
   {
      mock.setSomething(123);
      mock.setSomething(123);

      new VerificationsInOrder() {{ mock.setSomething(123); times = 2; }};
   }

   @Test
   public void verifyRepeatingInvocationThatOccursOneTimeMoreThanExpected()
   {
      thrown.expect(UnexpectedInvocation.class);

      mock.setSomething(123);
      mock.setSomething(123);

      new VerificationsInOrder() {{ mock.setSomething(123); maxTimes = 1; }};
   }

   @Test
   public void verifyRepeatingInvocationUsingMatcher()
   {
      mock.setSomething(123);
      mock.setSomething(45);

      new VerificationsInOrder() {{ mock.setSomething(anyInt); times = 2; }};
   }

   @Test
   public void verifyInvocationNotExpectedToOccurButWhichDoes()
   {
      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage("123");

      mock.prepare();
      mock.setSomething(123);

      new VerificationsInOrder() {{
         mock.prepare();
         mock.setSomething(anyInt); maxTimes = 0;
      }};
   }

   @Test
   public void verifyWithArgumentMatcher()
   {
      exerciseCodeUnderTest();

      new VerificationsInOrder() {{
         mock.prepare();
         mock.setSomething(anyInt);
      }};
   }

   @Test
   public void verifyWithIndividualInvocationCountsForNonConsecutiveInvocations()
   {
      exerciseCodeUnderTest();

      new VerificationsInOrder() {{
         mock.prepare(); times = 1;
         mock.setSomething(anyInt); times = 2;
      }};
   }

   @Test
   public void verifyUsingInvocationCountConstraintAndArgumentMatcherOnObjectWithMockedHashCode(
      @Mocked ClassWithHashCode wh)
   {
      mock.doSomething(null);
      mock.doSomething(wh);

      new VerificationsInOrder() {{
         mock.doSomething((ClassWithHashCode) withNull()); times = 1;
         mock.doSomething((ClassWithHashCode) withNotNull());
      }};
   }

   @Test
   public void verifyWithArgumentMatchersWhenOutOfOrder()
   {
      thrown.expect(MissingInvocation.class);
      thrown.expectMessage("any String");

      mock.setSomething(123);
      mock.setSomethingElse("anotherValue");
      mock.setSomething(45);

      new VerificationsInOrder() {{
         mock.setSomething(anyInt);
         mock.setSomething(anyInt);
         mock.setSomethingElse(anyString);
      }};
   }

   @Test
   public void verifyWithArgumentMatcherAndIndividualInvocationCountWhenOutOfOrder()
   {
      thrown.expect(MissingInvocation.class);
      thrown.expectMessage("Missing 1 invocation");
      thrown.expectMessage("any int");

      mock.setSomething(123);
      mock.prepare();
      mock.setSomething(45);

      new VerificationsInOrder() {{
         mock.prepare();
         mock.setSomething(anyInt); times = 2;
      }};
   }

   @Test
   public void verifyTwoIndependentSequencesOfInvocationsWhichOccurSeparately()
   {
      // First sequence:
      mock.setSomething(1);
      mock.setSomething(2);

      // Second sequence:
      mock.setSomething(10);
      mock.setSomething(20);

      // Verifies first sequence:
      new VerificationsInOrder() {{
         mock.setSomething(1);
         mock.setSomething(2);
      }};

      // Verifies second sequence:
      new VerificationsInOrder() {{
         mock.setSomething(10);
         mock.setSomething(20);
      }};
   }

   @Test
   public void verifyTwoIndependentSequencesOfInvocationsWhichAreMixedTogether()
   {
      mock.setSomething(1);  // first sequence
      mock.setSomething(10); // second sequence
      mock.setSomething(2);  // first sequence
      mock.setSomething(20); // second sequence

      // Verifies second sequence:
      new VerificationsInOrder() {{
         mock.setSomething(10);
         mock.setSomething(20);
      }};

      // Verifies first sequence:
      new VerificationsInOrder() {{
         mock.setSomething(1);
         mock.setSomething(2);
      }};
   }

   @Test
   public void verifySecondSequenceOfInvocationsWithTimesConstraintAfterVerifyingLastInvocationOfFirstSequence()
   {
      mock.setSomething(1); // first sequence
      mock.setSomething(3); // second sequence
      mock.setSomething(4); // second sequence
      mock.setSomething(2); // first sequence

      new VerificationsInOrder() {{
         mock.setSomething(1);
         mock.setSomething(2);
      }};

      new VerificationsInOrder() {{
         mock.setSomething(3);
         mock.setSomething(4); times = 1;
      }};
   }

   @Test
   public void verifyMethodInvokedOnOneOfTwoNewInstancesOfSameTypeWhenInvokedOnWrongInstance()
   {
      thrown.expect(MissingInvocation.class);
      thrown.expectMessage("save()");

      new Dependency();
      new Dependency().save();

      new VerificationsInOrder() {{
         Dependency d1 = new Dependency();
         new Dependency();
         d1.save();
      }};
   }
}

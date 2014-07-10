/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;

import mockit.internal.*;

public final class ExpectationsWithIterationsTest
{
   @SuppressWarnings("UnusedDeclaration")
   public static class Dependency
   {
      public void setSomething(int value) {}
      public void setSomethingElse(String value) {}
      public int editABunchMoreStuff() { return 1; }
      public boolean notifyBeforeSave() { return true; }
      public void prepare() {}
      public void save() {}
   }

   @Mocked private Dependency mock;

   private void exerciseCodeUnderTest()
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
   public void recordWithArgumentMatcherAndIndividualInvocationCounts()
   {
      new Expectations(1) {{
         mock.prepare(); maxTimes = 1;
         mock.setSomething(anyInt); minTimes = 1;
         mock.setSomethingElse(anyString); minTimes = 0; maxTimes = -1;
         mock.setSomething(anyInt); times = 1;
         mock.editABunchMoreStuff(); minTimes = 0; maxTimes = 5;
         mock.notifyBeforeSave(); maxTimes = -1;
         mock.save();
      }};

      exerciseCodeUnderTest();
   }

   @Test
   public void recordStrictInvocationsInIteratingBlock()
   {
      new Expectations(2) {{
         mock.setSomething(anyInt);
         mock.save();
      }};

      mock.setSomething(123);
      mock.save();
      mock.setSomething(45);
      mock.save();
   }

   @Test
   public void recordNonStrictInvocationsInIteratingBlock()
   {
      new NonStrictExpectations(2) {{
         mock.setSomething(anyInt);
         mock.save();
      }};

      mock.setSomething(123);
      mock.save();
      mock.setSomething(45);
      mock.save();
   }

   @Test(expected = MissingInvocation.class)
   public void recordInvocationInBlockWithWrongNumberOfIterations()
   {
      new Expectations(3) {{
         mock.setSomething(123);
      }};

      mock.setSomething(123);
   }

   @Test(expected = UnexpectedInvocation.class)
   public void recordInvocationInBlockWithNumberOfIterationsTooSmall()
   {
      new Expectations(2) {{
         mock.setSomething(123);
         mock.editABunchMoreStuff();
      }};

      for (int i = 0; i < 3; i++) {
         mock.setSomething(123);
         mock.editABunchMoreStuff();
      }
   }

   @Test
   public void recordWithArgumentMatcherAndIndividualInvocationCountsInIteratingBlock()
   {
      new Expectations(2) {{
         mock.prepare(); maxTimes = 1;
         mock.setSomething(anyInt); minTimes = 2;
         mock.editABunchMoreStuff(); minTimes = 1; maxTimes = 5;
         mock.save();
      }};

      for (int i = 0; i < 2; i++) {
         mock.prepare();
         mock.setSomething(123);
         mock.setSomething(45);
         mock.editABunchMoreStuff();
         mock.editABunchMoreStuff();
         mock.editABunchMoreStuff();
         mock.save();
      }
   }

   @Test
   public void recordRepeatingInvocationInIteratingBlock()
   {
      new Expectations(2) {{
         mock.setSomething(123); times = 2;
      }};

      mock.setSomething(123);
      mock.setSomething(123);
      mock.setSomething(123);
      mock.setSomething(123);
   }

   @Test
   public void recordInvocationsInASimpleBlockFollowedByAnIteratingOne()
   {
      new Expectations() {{ mock.setSomething(123); }};
      new Expectations(2) {{ mock.save(); }};

      mock.setSomething(123);
      mock.save();
      mock.save();
   }

   @SuppressWarnings("MethodWithMultipleLoops")
   @Test
   public void recordInvocationsInMultipleIteratingBlocks()
   {
      new Expectations(2) {{
         mock.setSomething(anyInt);
         mock.save();
      }};

      new Expectations(3) {{
         mock.prepare();
         mock.editABunchMoreStuff();
         mock.setSomethingElse(withNotEqual("")); minTimes = 0;
         mock.save();
      }};

      for (int i = 0; i < 2; i++) {
         mock.setSomething(123 + i);
         mock.save();
      }

      for (int i = 0; i < 3; i++) {
         mock.prepare();
         mock.editABunchMoreStuff();

         if (i != 1) {
            mock.setSomethingElse("" + i);
         }

         mock.save();
      }
   }
}

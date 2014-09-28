/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;
import org.junit.rules.*;

import mockit.internal.*;

public final class ExpectationsWithIterationsTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   @SuppressWarnings("unused")
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
   public void recordWithArgumentMatcherAndIndividualInvocationCounts()
   {
      new StrictExpectations(1) {{
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
      new StrictExpectations(2) {{
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
   public void recordInvocationInBlockWithWrongNumberOfIterations()
   {
      thrown.expect(MissingInvocation.class);

      new StrictExpectations(3) {{
         mock.setSomething(123);
      }};

      mock.setSomething(123);
   }

   @Test
   public void recordInvocationInBlockWithNumberOfIterationsTooSmall()
   {
      thrown.expect(UnexpectedInvocation.class);

      new StrictExpectations(2) {{
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
      new StrictExpectations(2) {{
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
      new StrictExpectations(2) {{
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
      new StrictExpectations() {{ mock.setSomething(123); }};
      new StrictExpectations(2) {{ mock.save(); }};

      mock.setSomething(123);
      mock.save();
      mock.save();
   }

   @SuppressWarnings("MethodWithMultipleLoops")
   @Test
   public void recordInvocationsInMultipleIteratingBlocks()
   {
      new StrictExpectations(2) {{
         mock.setSomething(anyInt);
         mock.save();
      }};

      new StrictExpectations(3) {{
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
            mock.setSomethingElse(String.valueOf(i));
         }

         mock.save();
      }
   }
}

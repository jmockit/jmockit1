/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package java8testing;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.junit.*;

import mockit.Mocked;
import org.jmockit.*;
import org.jmockit.Expectations.*;

@SuppressWarnings("Convert2Lambda")
@Ignore("Just for API design, no backing implementation yet")
public final class ExpectationWithInstanceFieldsTest
{
   @Mocked List<Object> mockList;

   @Test
   public void mockNotUsingLambdas(@Mocked Consumer<String> mockAction)
   {
      Expectations.record(new Block()
      {
         @Override
         public void perform(Spec s)
         {
            mockAction.accept(s.anyString); s.result = 1;
            mockAction.andThen(s.isNull()); s.result = new IOException(); s.times = 1;

            mockList.isEmpty(); s.result = true;
            mockList.remove(s.isSame("test")); s.result = true;

            mockList.sort(null); s.action = System.out::println;

            mockList.addAll(s.anyInt, s.isNotNull()); s.advice = (execution, args) -> execution.proceed();
         }
      });

      Expectations.record(mockList, new Block()
      {
         @Override
         public void perform(Spec e)
         {
            mockList.addAll(e.anyInt, null);
            e.delegate = args -> args.length > 0;
         }
      });

      mockAction.accept("");
      mockAction.andThen(System.out::println);
      mockList.clear();
      mockList.add(1);
      mockList.add(2, "testing");
      mockList.add(2);

      Expectations.verify(new Block() {
         @Override public void perform(Spec e) { mockList.add(e.anyInt, null); }
      });

      Expectations.verify(new Block()
      {
         @Override
         public void perform(Spec e)
         {
            mockAction.accept("");
            e.minTimes = 1;
            e.maxTimes = 2;
            mockAction.andThen(e.isNotNull());
            mockList.add(e.is(i -> i > 1), e.isNotNull());
         }
      });

      Expectations.verifyInOrder(new Block()
      {
         @Override
         public void perform(Spec e)
         {
            mockList.add(1);
            mockList.add(2);
         }
      });

      Expectations.verifyAll(mockAction, new Block()
      {
         @Override
         public void perform(Spec e)
         {
            mockAction.accept(e.anyString);
            mockAction.andThen(null);
         }
      });
   }

   @Test
   public void mockUsingLambdas(@Mocked Consumer<String> mockAction)
   {
      Expectations.record(e -> {
         mockAction.accept(e.anyString); e.result = 1;
         mockAction.andThen(e.isNull()); e.result = new IOException(); e.times = 1;

         mockList.isEmpty(); e.result = true;
         mockList.remove(e.isSame("test")); e.result = true;

         mockList.sort(null); e.action = System.out::println;

         mockList.addAll(e.anyInt, e.isNotNull()); e.advice = (execution, args) -> execution.proceed();
      });

      Expectations.record(mockList, e -> {
         mockList.addAll(e.anyInt, null);
         e.delegate = args -> args.length > 0;
      });

      mockAction.accept("");
      mockAction.andThen(System.out::println);
      mockList.clear();
      mockList.add(1);
      mockList.add(2, "testing");
      mockList.add(2);

      Expectations.verify(e -> mockList.add(e.anyInt, null));

      Expectations.verify(e -> {
         mockAction.accept("");
         e.minTimes = 1;
         e.maxTimes = 2;
         mockAction.andThen(e.isNotNull());
         mockList.add(e.is(i -> i > 1), e.isNotNull());
      });

      Expectations.verifyInOrder(e -> {
         mockList.add(1);
         mockList.add(2);
      });

      Expectations.verifyAll(mockAction, e -> {
         mockAction.accept(e.anyString);
         mockAction.andThen(null);
      });
   }
}

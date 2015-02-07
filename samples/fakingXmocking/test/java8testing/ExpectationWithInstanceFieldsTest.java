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
import static org.jmockit.Expectations.*;

@Ignore("Just for API design, no backing implementation yet")
public final class ExpectationWithInstanceFieldsTest
{
   Expectations exp;
   @Mocked List<Object> mockList;

   @Test
   public void recordAndVerifyExpectations(@Mocked Consumer<String> mockAction)
   {
      exp.record(e -> {
         mockAction.accept(e.anyString); e.result = 1;
         mockAction.andThen(e.isNull()); e.result = new IOException(); e.times = 1;

         mockList.isEmpty(); e.result = true;
         mockList.remove(e.isSame("test")); e.result = true;

         mockList.sort(null); e.action = System.out::println;

         mockList.addAll(e.anyInt, e.isNotNull()); e.advice = Execution::proceed;
      });

      exp.record(mockList, e -> { mockList.addAll(e.anyInt, null); e.delegate = args -> args.length > 0; });

      mockAction.accept("");
      mockAction.andThen(System.out::println);
      mockList.clear();
      mockList.add(1);
      mockList.add(2, "testing");
      mockList.add(2);

      exp.verify(e -> mockList.add(e.anyInt, null));

      exp.verify(e -> {
         mockAction.accept(""); e.minTimes = 1; e.maxTimes = 2;
         mockAction.andThen(e.isNotNull());
         mockList.add(e.is(i -> i > 1), e.isNotNull());
      });

      exp.verifyInOrder(e -> {
         mockList.add(1);
         mockList.add(2);
      });

      exp.verifyAll(mockAction, e -> {
         mockAction.accept(e.anyString);
         mockAction.andThen(null);
      });
   }
}

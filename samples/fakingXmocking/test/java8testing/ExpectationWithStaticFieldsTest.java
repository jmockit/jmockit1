/*
 * Copyright (c) 2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package java8testing;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.junit.*;

import mockit.*;

import static mockit.Expectation.*;

public final class ExpectationWithStaticFieldsTest
{
   @Mocked List<Object> mockList;

   @Test
   public void mockLambda(@Mocked Consumer<String> mockAction)
   {
      record(() -> {
         mockAction.accept(anyString); result = 1;
         mockAction.andThen(isNull()); result = new IOException(); times = 1;

         mockList.isEmpty(); result = true;
         mockList.remove(isSame("test")); result = true;

         mockList.sort(null); action = System.out::println;

         mockList.addAll(anyInt, isNotNull()); advice = (execution, args) -> execution.proceed();
      });

      record(mockList, () -> { mockList.addAll(anyInt, null); delegate = (args) -> args.length > 0; });

      mockAction.accept("");
      mockAction.andThen(System.out::println);
      mockList.clear();
      mockList.add(1);
      mockList.add(2, "testing");
      mockList.add(2);

      verify(() -> mockList.add(anyInt, null));

      verify(() -> {
         mockAction.accept(""); minTimes = 1; maxTimes = 2;
         mockAction.andThen(isNotNull());
         mockList.add(is(i -> i > 1), isNotNull());
      });

      verifyInOrder(() -> {
         mockList.add(1);
         mockList.add(2);
      });

      verifyAll(mockAction, () -> {
         mockAction.accept(anyString);
         mockAction.andThen(null);
      });
   }
}

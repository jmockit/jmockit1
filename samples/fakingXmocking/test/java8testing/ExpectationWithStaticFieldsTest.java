/*
 * Copyright (c) 2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package java8testing;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

import static org.hamcrest.Matchers.*;
import static org.jmockit.Expectation.*;

@Ignore("Just for API design, no backing implementation yet")
public final class ExpectationWithStaticFieldsTest
{
   static class Dependency {}
   static class Value {}

   @Mocked List<Object> mockList;
   @Mocked Dependency dependency;

   @Test
   public void recordAndVerifyExpectations(@Mocked Consumer<String> mockAction)
   {
      Value value = new Value();

      record(() -> {
         mockAction.accept(any()); result = 1;
         mockAction.andThen(null); result = new IOException(); times = 1;

         mockList.isEmpty(); result = true;
         mockList.remove(same(value)); result = true;

         mockList.sort(null); action = System.out::println;

         mockList.addAll(any(), notNull()); advice = Execution::proceed;
      });

      record(mockList, () -> { mockList.addAll(any(), null); delegate = args -> args.length > 0; });

      mockAction.accept("");
      mockAction.andThen(System.out::println);
      mockList.clear();
      mockList.add(1);
      mockList.add(2, "testing");
      mockList.add(2);
      mockList.remove(value);

      assertThat(mockList, is(not(empty())));
      assertThat(mockList.get(0), is(any(Object.class)));

      verify(() -> mockList.add(any(), null));

      verify(() -> {
         mockAction.accept(""); minTimes = 1; maxTimes = 2;
         mockAction.andThen(notNull());
         mockList.add(as(i -> i > 1), notNull());
         mockList.add(as(item -> item instanceof String));
      });

      verifyInOrder(() -> {
         mockList.add(1);
         mockList.add(2);
      });

      verifyAll(mockAction, () -> {
         mockAction.accept(any());
         mockAction.andThen(null);
      });
   }
}

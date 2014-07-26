/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package java8testing;

import java.util.*;
import java.util.stream.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

public final class Java8EmptyReturnsTest
{
   @Test
   public void mockMethodsReturningJava8ObjectsWhichCanBeEmpty(
      @Injectable Stream<String> stream, @Injectable Stream<Integer> intStream,
      @Injectable Stream<Long> longStream, @Injectable Stream<Double> doubleStream)
   {
      Optional<String> any = stream.findAny();
      assertSame(Optional.empty(), any);

      Stream<String> distinct = stream.distinct();
      assertSame(Stream.empty(), distinct);

      Spliterator<String> spliterator = stream.spliterator();
      assertSame(Spliterators.emptySpliterator(), spliterator);

      Spliterator<Integer> intSpliterator = intStream.spliterator();
      assertSame(Spliterators.emptyIntSpliterator(), intSpliterator);

      Spliterator<Long> longSpliterator = longStream.spliterator();
      assertSame(Spliterators.emptyLongSpliterator(), longSpliterator);

      Spliterator<Double> doubleSpliterator = doubleStream.spliterator();
      assertSame(Spliterators.emptyDoubleSpliterator(), doubleSpliterator);
   }
}

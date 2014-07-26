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
      @Injectable Stream<?> stream, @Injectable Stream<Integer> streamOfIntegers,
      @Injectable Stream<Long> streamOfLongs, @Injectable Stream<Double> streamOfDoubles)
   {
      Optional<?> any = stream.findAny();
      assertSame(Optional.empty(), any);

      // Stream.empty() always creates a new stream, so it can't be used here.
      Stream<?> distinct = stream.distinct();
      assertFalse(distinct.iterator().hasNext());

      Spliterator<?> spliterator = stream.spliterator();
      assertSame(Spliterators.emptySpliterator(), spliterator);

      Spliterator<Integer> intSpliterator = streamOfIntegers.spliterator();
      assertEquals(0, intSpliterator.estimateSize());

      Spliterator<Long> longSpliterator = streamOfLongs.spliterator();
      assertEquals(0, longSpliterator.estimateSize());

      Spliterator<Double> doubleSpliterator = streamOfDoubles.spliterator();
      assertEquals(0, doubleSpliterator.estimateSize());

      assertSame(Collections.emptyIterator(), stream.iterator());
   }

   @Test
   public void mockMethodsReturningJava8PrimitiveSpecializationsWhichCanBeEmpty(
      @Injectable IntStream intStream, @Injectable LongStream longStream, @Injectable DoubleStream doubleStream)
   {
      assertSame(OptionalInt.empty(), intStream.max());
      assertSame(OptionalLong.empty(), longStream.min());
      assertSame(OptionalDouble.empty(), doubleStream.findFirst());

      assertEquals(0, intStream.sorted().count());
      assertEquals(0, longStream.sequential().count());
      assertEquals(0, doubleStream.distinct().count());

      assertSame(Spliterators.emptyIntSpliterator(), intStream.spliterator());
      assertSame(Spliterators.emptyLongSpliterator(), longStream.spliterator());
      assertSame(Spliterators.emptyDoubleSpliterator(), doubleStream.spliterator());

      assertFalse(intStream.iterator().hasNext());
      assertFalse(longStream.iterator().hasNext());
      assertFalse(doubleStream.iterator().hasNext());
   }
}

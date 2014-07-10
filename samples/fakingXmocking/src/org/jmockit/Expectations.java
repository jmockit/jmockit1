/*
 * Copyright (c) 2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jmockit;

import java.util.function.*;

/**
 * Proposed future Java 8 API for the recording & verification of expectations.
 */
public final class Expectations
{
   public interface Block { void perform(Spec s); }
   public interface Action { void perform(Object... args); }
   public interface Delegate { Object delegate(Object... args); }
   public interface Execution { Object proceed(Object... replacementArgs); }
   public interface Advice { Object advice(Execution execution, Object... args); }

   private Expectations() {}

   public static void record(Block expectations) { expectations.perform(new Spec()); }
   public static void record(Object toPartiallyMock, Block expectations) {}
   public static void record(Class<?> toPartiallyMock, Block expectations) {}

   public static void verify(Block expectations) { expectations.perform(new Spec()); }
   public static void verifyInOrder(Block expectations) {}
   public static void verifyAll(Block expectations) {}
   public static void verifyAll(Object mock, Block expectations) {}
   public static void verifyAll(Class<?> mockedType, Block expectations) {}

   public static final class Spec
   {
      public Object result;
      public Action action;
      public Delegate delegate;
      public Advice advice;

      public int times;
      public int minTimes;
      public int maxTimes;

      public final String anyString = "";
      public final Boolean anyBoolean = false;
      public final Character anyChar = 0;
      public final Byte anyByte = 0;
      public final Short anyShort = 0;
      public final Integer anyInt = 0;
      public final Long anyLong = 0L;
      public final Float anyFloat = 0.0F;
      public final Double anyDouble = 0.0;

      public <T> T isNull() { return null; }
      public <T> T isNotNull() { return null; }
      public <T> T isSame(T instance) { return null; }
      public <T> T is(Predicate<? super T> predicate) { return null; }
   }
}

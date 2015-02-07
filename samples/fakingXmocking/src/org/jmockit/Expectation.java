/*
 * Copyright (c) 2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jmockit;

import java.util.function.*;

/**
 * Proposed future Java 8 API for the recording & verification of expectations.
 */
public final class Expectation
{
   @FunctionalInterface
   public interface Action { void perform(Object... args); }

   @FunctionalInterface
   public interface Delegate { Object delegate(Object... args); }

   @FunctionalInterface
   public interface Execution { Object proceed(Object... replacementArgs); }

   @FunctionalInterface
   public interface Advice { Object advice(Execution execution, Object... args); }

   private Expectation() {}

   public static void record(Runnable expectations) { expectations.run(); }
   public static void record(Object partialMock, Runnable expectations) { expectations.run(); }
   public static void record(Class<?> partiallyMockedType, Runnable expectations) { expectations.run(); }

   public static void verify(Runnable expectations) { expectations.run(); }
   public static void verifyInOrder(Runnable expectations) { expectations.run(); }
   public static void verifyAll(Runnable expectations) { expectations.run(); }
   public static void verifyAll(Object mock, Runnable expectations) { expectations.run(); }
   public static void verifyAll(Class<?> mockedType, Runnable expectations) { expectations.run(); }

   public static Object result;
   public static Action action;
   public static Delegate delegate;
   public static Advice advice;

   public static int times;
   public static int minTimes;
   public static int maxTimes;

   public static final String anyString = "";
   public static final Boolean anyBoolean = false;
   public static final Character anyChar = 0;
   public static final Byte anyByte = 0;
   public static final Short anyShort = 0;
   public static final Integer anyInt = 0;
   public static final Long anyLong = 0L;
   public static final Float anyFloat = 0.0F;
   public static final Double anyDouble = 0.0;

   public static <T> T isNull() { return null; }
   public static <T> T isNotNull() { return null; }
   public static <T> T isSame(T instance) { return null; }
   public static <T> T is(Predicate<? super T> predicate) { return null; }
}

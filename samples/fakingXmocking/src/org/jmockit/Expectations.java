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
   @FunctionalInterface
   public interface Block { void perform(Spec s); }

   @FunctionalInterface
   public interface Action { void perform(Object... args); }

   @FunctionalInterface
   public interface Delegate { Object delegate(Object... args); }

   @FunctionalInterface
   public interface Execution { Object proceed(Object... replacementArgs); }

   @FunctionalInterface
   public interface Advice { Object advice(Execution execution, Object... args); }

   private Expectations() {}

   public void record(Block expectations) { expectations.perform(new Spec()); }
   public void record(Object toPartiallyMock, Block expectations) {}
   public void record(Class<?> toPartiallyMock, Block expectations) {}

   public void verify(Block expectations) { expectations.perform(new Spec()); }
   public void verifyInOrder(Block expectations) {}
   public void verifyAll(Block expectations) {}
   public void verifyAll(Object mock, Block expectations) {}
   public void verifyAll(Class<?> mockedType, Block expectations) {}

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

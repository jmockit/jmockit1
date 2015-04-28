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
   public interface Block<S extends Spec> { void perform(S s); }

   @FunctionalInterface
   public interface Action { void perform(Object... args); }

   @FunctionalInterface
   public interface Delegate { Object delegate(Object... args); }

   @FunctionalInterface
   public interface Execution { Object proceed(Object... replacementArgs); }

   @FunctionalInterface
   public interface Advice { Object advice(Execution execution, Object... args); }

   private Expectations() {}

   public void record(Block<RecordingSpec> expectations) { expectations.perform(new RecordingSpec()); }
   public void record(Object toPartiallyMock, Block<RecordingSpec> expectations) { expectations.perform(new RecordingSpec());}
   public void record(Class<?> toPartiallyMock, Block<RecordingSpec> expectations) { expectations.perform(new RecordingSpec());}

   public void verify(Block<VerificationSpec> expectations) { expectations.perform(new VerificationSpec()); }
   public void verifyInOrder(Block<VerificationSpec> expectations) { expectations.perform(new VerificationSpec()); }
   public void verifyAll(Block<VerificationSpec> expectations) { expectations.perform(new VerificationSpec()); }
   public void verifyAll(Object mock, Block<VerificationSpec> expectations) { expectations.perform(new VerificationSpec()); }
   public void verifyAll(Class<?> mockedType, Block<VerificationSpec> expectations) { expectations.perform(new VerificationSpec()); }

   public static class Spec
   {
      private static final String EMPTY = new String();

      public int times;
      public int minTimes;
      public int maxTimes;


      public final String anyString = EMPTY;
      public final Boolean anyBoolean = false;
      public final Character anyChar = 0;
      public final Byte anyByte = 0;
      public final Short anyShort = 0;
      public final Integer anyInt = 0;
      public final Long anyLong = 0L;
      public final Float anyFloat = 0.0F;
      public final Double anyDouble = 0.0;

      Spec() {}

      public <T> T isNull() { return null; }
      public <T> T isNotNull() { return null; }
      public <T> T isSame(T instance) { return null; }
      public <T> T is(Predicate<? super T> predicate) { return null; }
   }

   public static final class RecordingSpec extends Spec
   {
      public Object result;
      public Action action;
      public Delegate delegate;
      public Advice advice;

      RecordingSpec() {}
   }

   public static final class VerificationSpec extends Spec
   {
      VerificationSpec() {}
   }
}

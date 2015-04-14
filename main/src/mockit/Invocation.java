/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.lang.reflect.*;
import javax.annotation.*;

import mockit.internal.*;

/**
 * A context object representing the current invocation to a mocked method/constructor, to be passed as the
 * <em>first</em> parameter of the corresponding delegate/mock method implementation.
 * <p/>
 * With the <em>Expectations</em> API, this parameter can appear in delegate methods implemented in {@link Delegate}
 * classes.
 * With the <em>Mockups</em> API, it can appear in {@link Mock @Mock} methods.
 *
 * @see #getInvokedInstance()
 * @see #getInvokedArguments()
 * @see #getInvocationCount()
 * @see #getInvocationIndex()
 * @see #getMinInvocations()
 * @see #getMaxInvocations()
 * @see #proceed(Object...)
 * @see <a href="http://jmockit.org/tutorial/BehaviorBasedTesting.html#delegates">Tutorial (expectations)</a>
 * @see <a href="http://jmockit.org/tutorial/StateBasedTesting.html#invocation">Tutorial (mockups)</a>
 */
public class Invocation
{
   @Nullable private final Object invokedInstance;
   private final Object[] invokedArguments;
   private final int invocationCount;
   private final int minInvocations;
   private final int maxInvocations;

   /**
    * For internal use only.
    */
   protected Invocation(
      @Nullable Object invokedInstance, @Nonnull Object[] invokedArguments,
      int invocationCount, int minInvocations, int maxInvocations)
   {
      this.invokedInstance = invokedInstance;
      this.invokedArguments = invokedArguments;
      this.invocationCount = invocationCount;
      this.minInvocations = minInvocations;
      this.maxInvocations = maxInvocations;
   }

   /**
    * Returns the instance on which the current invocation was made, or {@code null} for a {@code static} method
    * invocation.
    */
   public final <T> T getInvokedInstance()
   {
      //noinspection unchecked,ConstantConditions
      return (T) invokedInstance;
   }

   /**
    * Returns the {@code Method} or {@code Constructor} object corresponding to the mocked method or constructor,
    * respectively.
    */
   public final <M extends Member> M getInvokedMember()
   {
      //noinspection unchecked,ClassReferencesSubclass
      return (M) ((BaseInvocation) this).getRealMember();
   }

   /**
    * Returns the actual argument values passed in the invocation to the mocked method/constructor.
    */
   public final Object[] getInvokedArguments() { return invokedArguments; }

   /**
    * Returns the current invocation count. The first invocation starts at 1 (one).
    */
   public final int getInvocationCount() { return invocationCount; }

   /**
    * Returns the index for the current invocation. The first invocation starts at 0 (zero).
    * Note that this is equivalent to {@link #getInvocationCount()} - 1.
    */
   public final int getInvocationIndex() { return invocationCount - 1; }

   /**
    * Returns the current value of the minimum invocation count associated with the matching expectation or mock method.
    * <p/>
    * For an expectation, this call will return the value specified through the
    * {@linkplain Invocations#times times} or {@linkplain Invocations#minTimes minTimes} field, if that was the case;
    * if not, the value will be {@code 1} for a regular or strict expectation, or {@code 0} for a non-strict one.
    * For a {@code @Mock} method, it will return the value specified for the {@linkplain Mock#invocations invocations}
    * or {@linkplain Mock#minInvocations minInvocations} attribute, or {@code 0} if none.
    */
   public final int getMinInvocations() { return minInvocations; }

   /**
    * Returns the current value of the maximum invocation count for the matching expectation or mock method
    * (<code>-1</code> indicates that it's unlimited).
    * <p/>
    * For an expectation, this call will return the value specified through the
    * {@linkplain Invocations#times times} or {@linkplain Invocations#maxTimes maxTimes} field, if that was the case;
    * if not, the value will be {@code -1} for a regular or non-strict expectation, or {@code 1} for a strict one.
    * For a {@code @Mock} method, it will return the value specified for the {@linkplain Mock#invocations invocations}
    * or {@linkplain Mock#maxInvocations maxInvocations} attribute, or {@code -1} if none.
    */
   public final int getMaxInvocations() { return maxInvocations; }

   /**
    * Allows execution to proceed into the real implementation of the mocked method/constructor.
    * <p/>
    * In the case of a mocked method, the real implementation is executed with the argument values originally received
    * or explicitly given as replacement.
    * Whatever comes out of that call (either a return value or a thrown exception/error, even if it is a
    * <em>checked</em> exception) becomes the result of the current invocation to the mock method.
    * <p/>
    * In the case of a mocked constructor, the real constructor implementation code which comes after the necessary call
    * to "<code>super</code>" is executed, using the original argument values; replacement arguments are not supported.
    * If the execution of said code throws an exception or error, it is propagated out to the caller of the mocked
    * constructor (even in the case of a <em>checked</em> exception).
    * Contrary to proceeding into a mocked method, it's not possible to actually execute test code inside the delegate
    * method after proceeding into the real constructor, nor to proceed into it more than once.
    *
    * @param replacementArguments the argument values to be passed to the real method, as replacement for the values
    *                             received by the mock method; if those received values should be passed without
    *                             replacement, then this method should be called with no values
    * @param <T> the return type of the mocked method
    *
    * @return the same value returned by the real method, if any
    *
    * @throws UnsupportedOperationException if attempting to proceed into a mocked method which does not belong to an
    * {@linkplain Injectable injectable mocked type} nor to a {@linkplain Expectations#Expectations(Object...) dynamic
    * partially mocked type}, into a {@code native} method, or into a mocked constructor while passing replacement
    * arguments
    *
    * @see <a href="http://jmockit.org/tutorial/StateBasedTesting.html#proceed">Tutorial</a>
    */
   public final <T> T proceed(Object... replacementArguments)
   {
      //noinspection ClassReferencesSubclass
      return ((BaseInvocation) this).doProceed(replacementArguments);
   }
}

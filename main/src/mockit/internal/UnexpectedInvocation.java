/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal;

/**
 * Thrown to indicate that one or more unexpected invocations occurred during the test.
 */
public final class UnexpectedInvocation extends Error
{
   public UnexpectedInvocation(String detailMessage)
   {
      super(detailMessage);
   }
}

/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal;

/**
 * Thrown to indicate that one or more expected invocations still had not occurred by the end of the test.
 */
public final class MissingInvocation extends Error
{
   public MissingInvocation(String detailMessage)
   {
      super(detailMessage);
   }
}

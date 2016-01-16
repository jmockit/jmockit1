/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.util;

public final class VisitInterruptedException extends RuntimeException
{
   public static final VisitInterruptedException INSTANCE = new VisitInterruptedException();

   private VisitInterruptedException() {}
}

/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.modification;

final class VisitInterruptedException extends RuntimeException
{
   static final VisitInterruptedException INSTANCE = new VisitInterruptedException();
}

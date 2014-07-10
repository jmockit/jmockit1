/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration;

import java.util.*;

import mockit.*;

public final class CollaboratorStrictExpectations extends Expectations
{
   public CollaboratorStrictExpectations(Collaborator mock)
   {
      mock.doSomething(); result = new IllegalFormatCodePointException('x');
      times = 2;
   }
}

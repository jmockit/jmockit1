/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration;

import java.util.*;

import mockit.*;

public final class CollaboratorExpectations extends Expectations
{
   public CollaboratorExpectations(Collaborator mock)
   {
      new Collaborator(); maxTimes = 1;

      mock.doSomething(); result = new IllegalFormatCodePointException('x');
      minTimes = 2;
   }
}

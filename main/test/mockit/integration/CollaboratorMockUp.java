/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration;

import java.util.*;

import mockit.*;

public final class CollaboratorMockUp extends MockUp<Collaborator>
{
   @Mock(maxInvocations = 1)
   public void $init() {}

   @Mock(minInvocations = 2)
   public void doSomething() { throw new IllegalFormatCodePointException('x'); }
}

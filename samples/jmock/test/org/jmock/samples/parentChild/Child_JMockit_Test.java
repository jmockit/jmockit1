/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jmock.samples.parentChild;

import org.junit.*;

import mockit.*;

/**
 * Notice how much simpler the equivalent test is with JMockit.
 * <p/>
 * The jMock "states" API is used when one needs to "constrain the order of invocations", by accounting for each
 * individual invocation only when its associated state is active (see jMock API documentation).
 * Another use for this API, as demonstrated in {@code ChildTest}, is to cause certain invocations to be ignored (ie,
 * they will be allowed to occur inside the code under test, when otherwise they wouldn't unless explicitly recorded).
 * <p/>
 * In JMockit, the main use case for the jMock state-machine, namely to constrain the order of invocations, is fulfilled
 * through the {@code VerificationsInOrder} API.
 * Each state-machine in a jMock test imposes a partial ordering among a subset of the mocked invocations, which is
 * equivalent to having one ordered verification block in the corresponding JMockit test; so, multiple state-machines
 * would correspond to multiple ordered verification blocks.
 * The secondary use case (ignoring certain invocations), is fulfilled in JMockit by simply not worrying about such
 * invocations, since they are allowed by default (unless using strict expectations, of course, but that is only for
 * tests which do not want to ignore any invocations).
 */
public final class Child_JMockit_Test
{
   @Injectable Parent parent;
   @Tested Child child;

   @Test
   public void removesItselfFromOldParentWhenAssignedNewParent(@Injectable final Parent newParent)
   {
      child.reparent(newParent);

      new VerificationsInOrder() {{
         parent.removeChild(child);
         newParent.addChild(child);
      }};
   }
}

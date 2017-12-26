/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package otherTests;

import mockit.integration.*;

public final class TestedClass
{
   private final MockedClass dependency;

   public TestedClass(MockedClass dependency) { this.dependency = dependency; }
   public boolean doSomething(int i) { return dependency.doSomething(i); }
}

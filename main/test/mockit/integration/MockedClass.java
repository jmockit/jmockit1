/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration;

public final class MockedClass
{
   public String getValue() { return "REAL"; }
   public boolean doSomething(int i) { return i > 0; }
   public boolean doSomethingElse(int i) { return i < 0; }
}

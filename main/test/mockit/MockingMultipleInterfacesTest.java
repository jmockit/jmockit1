/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;

import org.junit.*;

import static org.junit.Assert.*;

import mockit.MockingMultipleInterfacesTest.Dependency;

public final class MockingMultipleInterfacesTest<MultiMock extends Dependency & Runnable>
{
   interface Dependency
   {
      String doSomething(boolean b);
   }

   @Mocked MultiMock multiMock;

   @Test
   public void mockFieldWithTwoInterfaces()
   {
      new Expectations() {{ multiMock.doSomething(false); result = "test"; }};

      multiMock.run();
      assertEquals("test", multiMock.doSomething(false));

      new Verifications() {{ multiMock.run(); }};
   }

   @Test
   public <M extends Dependency & Serializable> void mockParameterWithTwoInterfaces(@Mocked final M mock)
   {
      new Expectations() {{ mock.doSomething(true); result = "test"; }};

      assertEquals("test", mock.doSomething(true));
   }

   public interface Base { void doSomething(); }
   abstract static class Derived implements Base { protected Derived() {} }
   public abstract static class ToBeMocked extends Derived {}

   @Test
   public void mockAbstractMethodInheritedFromInterfaceImplementedBySuperClass(@Mocked final ToBeMocked mock)
   {
      mock.doSomething();

      new Verifications() {{ mock.doSomething(); times = 1; }};
   }
}

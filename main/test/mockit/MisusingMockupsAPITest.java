/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.applet.*;

import org.junit.*;
import org.junit.rules.*;
import static org.junit.Assert.*;

public final class MisusingMockupsAPITest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   @Test
   public void mockSameMethodTwiceWithReentrantMocksFromTwoDifferentMockClasses()
   {
      new MockUp<Applet>() {
         @Mock
         int getComponentCount(Invocation inv)
         {
            int i = inv.proceed();
            return i + 1;
         }
      };

      int i = new Applet().getComponentCount();
      assertEquals(1, i);

      new MockUp<Applet>() {
         @Mock
         int getComponentCount(Invocation inv)
         {
            int j = inv.proceed();
            return j + 2;
         }
      };

      // Should return 3, but returns 5. Chaining mock methods is not supported.
      int j = new Applet().getComponentCount();
      assertEquals(5, j);
   }

   public interface AnInterface { void doSomething(); }

   @Test
   public void attemptToProceedIntoEmptyMethodOfPublicInterface()
   {
      thrown.expect(UnsupportedOperationException.class);
      thrown.expectMessage("Cannot proceed");
      thrown.expectMessage("interface method");

      AnInterface mock = new MockUp<AnInterface>() {
         @Mock
         void doSomething(Invocation invocation) { invocation.proceed(); }
      }.getMockInstance();

      mock.doSomething();
   }
}
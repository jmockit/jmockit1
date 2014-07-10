package org.easymock.documentation;

import org.junit.*;
import static org.junit.Assert.*;

import org.easymock.*;
import static org.easymock.EasyMock.*;
import org.easymock.samples.*;

public final class DocumentationExamplesTest extends EasyMockSupport
{
   @Test
   public void changingBehaviorForTheSameMethodCall()
   {
      Collaborator mock = createMock(Collaborator.class);
      String title = "Document";

      expect(mock.voteForRemoval(title))
        .andReturn(42).times(3)
        .andThrow(new RuntimeException())
        .andReturn(-42);
      replayAll();

      assertEquals(42, mock.voteForRemoval(title));
      assertEquals(42, mock.voteForRemoval(title));
      assertEquals(42, mock.voteForRemoval(title));

      try {
         mock.voteForRemoval(title);
      }
      catch (RuntimeException e) {
         // OK
      }

      assertEquals(-42, mock.voteForRemoval(title));
      verifyAll();
   }

   @Test
   public void checkingMethodCallOrderBetweenMocks()
   {
      IMocksControl ctrl = createStrictControl();
      MyInterface mock1 = ctrl.createMock(MyInterface.class);
      MyInterface mock2 = ctrl.createMock(MyInterface.class);

      mock1.a();
      mock2.a();

      ctrl.checkOrder(false);

      mock1.c(); expectLastCall().anyTimes();
      mock2.c(); expectLastCall().anyTimes();

      ctrl.checkOrder(true);

      mock2.b();
      mock1.b();

      ctrl.replay();

      // Ordered:
      mock1.a();
      mock2.a();

      // Unordered:
      mock2.c();
      mock1.c();
      mock2.c();

      // Ordered:
      mock2.b();
      mock1.b();

      ctrl.verify();
   }
}

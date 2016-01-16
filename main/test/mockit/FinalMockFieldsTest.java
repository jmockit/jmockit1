/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import static org.junit.Assert.*;
import org.junit.*;

public final class FinalMockFieldsTest
{
   static final class Collaborator
   {
      Collaborator() {}
      Collaborator(boolean b) { if (!b) throw new IllegalArgumentException(); }
      int getValue() { return -1; }
      void doSomething() {}
   }

   static final class AnotherCollaborator
   {
      int getValue() { return -1; }
      void doSomething() {}
   }

   @Injectable final Collaborator mock = new Collaborator();
   @Mocked final AnotherCollaborator mock2 = new AnotherCollaborator();

   @Before
   public void useMockedTypes()
   {
      assertEquals(0, mock.getValue());
      assertEquals(0, mock2.getValue());
      assertEquals(0, YetAnotherCollaborator.doSomethingStatic());
   }

   @Test
   public void recordExpectationsOnInjectableFinalMockField()
   {
      new Expectations() {{
         mock.getValue(); result = 12;
         mock.doSomething(); times = 0;
      }};

      assertEquals(12, mock.getValue());
   }

   @Test
   public void recordExpectationsOnFinalMockField()
   {
      AnotherCollaborator collaborator = new AnotherCollaborator();

      new Expectations() {{
         mock2.doSomething(); times = 1;
      }};

      collaborator.doSomething();
      assertEquals(0, collaborator.getValue());
   }

   @Mocked final ProcessBuilder mockProcessBuilder = null;

   @Test
   public void recordExpectationsOnConstructorOfFinalMockField()
   {
      new Expectations() {{
         new ProcessBuilder("test"); times = 1;
      }};

      new ProcessBuilder("test");
   }

   static final class YetAnotherCollaborator
   {
      YetAnotherCollaborator(boolean b) { if (!b) throw new IllegalArgumentException(); }
      int getValue() { return -1; }
      void doSomething() {}
      static int doSomethingStatic() { return -2; }
   }

   @Mocked final YetAnotherCollaborator unused = null;

   @Test
   public void recordExpectationsOnStaticMethodAndConstructorOfFinalLocalMockField()
   {
      new Expectations() {{
         new YetAnotherCollaborator(true); result = new RuntimeException();
         YetAnotherCollaborator.doSomethingStatic(); result = 123;
      }};

      try {
         new YetAnotherCollaborator(true);
         fail();
      }
      catch (RuntimeException ignore) {}

      assertEquals(123, YetAnotherCollaborator.doSomethingStatic());
   }
}

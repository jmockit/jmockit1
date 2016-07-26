/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.logging.*;
import javax.annotation.*;
import javax.ejb.*;
import javax.enterprise.context.*;
import javax.inject.*;

import org.junit.*;
import static org.junit.Assert.*;

import org.springframework.beans.factory.annotation.*;

public final class TestedClassWithFullAnnotatedDITest
{
   static final class TestedClass
   {
      @Inject Runnable action;
      @Autowired ItfWithSingleLoadedImpl dependency1;
      @Resource ItfWithSingleLoadedImpl dependency2;
      @Inject ItfWithTwoImplsButOnlyOneLoaded anotherDependency;
      @Inject private Logger log1;
      @Inject private Logger log2;
      Logger log3;
      Value value;
      Collaborator collaborator;
      @Inject Conversation conversation;
   }

   static final class Value {}
   static class Collaborator {}

   public interface ItfWithSingleLoadedImpl {}
   public static final class SingleLoadedImpl implements ItfWithSingleLoadedImpl { @EJB ItfToBeMocked ejb; }

   public interface ItfWithTwoImplsButOnlyOneLoaded {}
   @SuppressWarnings("unused")
   public static final class AnotherImpl1 implements ItfWithTwoImplsButOnlyOneLoaded {}
   public static final class AnotherImpl2 implements ItfWithTwoImplsButOnlyOneLoaded {}

   public interface ItfToBeMocked {}

   @Tested SingleLoadedImpl dep1;
   @Tested AnotherImpl2 anotherDep;
   @Tested Collaborator collaborator;
   @Tested(fullyInitialized = true) TestedClass tested;
   // Without these injectables, a "missing @Injectable" exception occurs for each unresolved field.
   @Injectable Runnable action;
   @Injectable ItfToBeMocked ejb;

   @Test
   public void injectInitializedDependenciesForInterfacesHavingASingleLoadedImplementationClass()
   {
      assertSame(action, tested.action);
      assertNotNull(tested.dependency1);
      assertSame(tested.dependency1, tested.dependency2);
      assertTrue(tested.anotherDependency instanceof AnotherImpl2);
      assertSame(ejb, ((SingleLoadedImpl) tested.dependency1).ejb);
   }

   @Test
   public void injectLoggerFieldsWithLoggerCreatedWithTestedClassName()
   {
      assertEquals(TestedClass.class.getName(), tested.log1.getName());
      assertSame(tested.log2, tested.log1);
   }

   @Test
   public void leaveNonAnnotatedFieldsUninitializedUnlessThereIsAMatchingTestedField()
   {
      assertNull(tested.value);
      assertNull(tested.log3);
      assertSame(collaborator, tested.collaborator);
   }

   @Tested Conversation conversation;

   @Test
   public void manageConversationContext()
   {
      assertNotNull(conversation);
      assertSame(tested.conversation, conversation);
      assertTrue(conversation.isTransient());

      assertEquals(0, conversation.getTimeout());
      conversation.setTimeout(1500);
      assertEquals(1500, conversation.getTimeout());

      assertNull(conversation.getId());

      conversation.begin();
      assertFalse(conversation.isTransient());
      assertNotNull(conversation.getId());

      conversation.end();
      assertTrue(conversation.isTransient());
      assertNull(conversation.getId());

      conversation.begin("test");
      assertFalse(conversation.isTransient());
      assertEquals("test", conversation.getId());
   }
}

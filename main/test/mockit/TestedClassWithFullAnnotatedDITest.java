/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.logging.*;
import javax.annotation.*;
import javax.ejb.*;
import javax.inject.*;

import org.junit.*;
import static org.junit.Assert.*;

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
   }

   public interface ItfWithSingleLoadedImpl {}
   public static final class SingleLoadedImpl implements ItfWithSingleLoadedImpl { @EJB ItfWithTwoLoadedImpls ejb; }

   public interface ItfWithTwoImplsButOnlyOneLoaded {}
   @SuppressWarnings("unused")
   public static final class AnotherImpl1 implements ItfWithTwoImplsButOnlyOneLoaded {}
   public static final class AnotherImpl2 implements ItfWithTwoImplsButOnlyOneLoaded {}

   public interface ItfWithTwoLoadedImpls {}
   public static final class YetAnotherImpl1 implements ItfWithTwoLoadedImpls {}
   public static final class YetAnotherImpl2 implements ItfWithTwoLoadedImpls {}

   @BeforeClass
   public static void loadImplementationClasses()
   {
      SingleLoadedImpl.class.getName();
      AnotherImpl2.class.getName();
      YetAnotherImpl1.class.getName();
      YetAnotherImpl2.class.getName();
   }

   @Tested(fullyInitialized = true) TestedClass tested;
   // Without these injectables, a "missing @Injectable" exception occurs for each unresolved field.
   @Injectable Runnable action;
   @Injectable ItfWithTwoLoadedImpls ejb;

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
}

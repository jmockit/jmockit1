/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class TestedMethodsTest
{
   public interface Dependency {}
   public interface AnotherDependency {}
   static class DependencyImpl implements Dependency {}
   @SuppressWarnings("unused") public interface BaseDAO<T extends Serializable> {}
   public interface ConcreteDAO extends BaseDAO<String> {}
   static class DAOImpl implements ConcreteDAO {}
   static class TestedClass { Dependency dependency; ConcreteDAO dao; AnotherDependency anotherDependency; }

   @Tested
   static Class<? extends Dependency> resolveDependencyInterfaces(Class<Dependency> dependencyInterface)
   {
      assertSame(Dependency.class, dependencyInterface);
      return DependencyImpl.class;
   }

   @Tested
   Class<?> resolveDAOInterfaces(Class<? extends BaseDAO<?>> daoInterface)
   {
      assertSame(ConcreteDAO.class, daoInterface);
      return DAOImpl.class;
   }

   @Tested(fullyInitialized = true) TestedClass tested;

   @Test
   public void injectInterfaceImplementationsFromClassesReturnedFromTestedMethods()
   {
      assertTrue(tested.dependency instanceof DependencyImpl);
      assertTrue(tested.dao instanceof DAOImpl);
      assertNull(tested.anotherDependency);
   }
}

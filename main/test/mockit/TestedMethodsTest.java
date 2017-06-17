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
   static class DependencyImpl implements Dependency {}
   @SuppressWarnings("unused") public interface BaseDAO<T extends Serializable> {}
   static class DAOImpl implements BaseDAO<String> {}
   static class TestedClass { Dependency dependency; BaseDAO<String> dao; }

   @Tested
   static Class<? extends Dependency> resolveDependencyInterfaces(Class<Dependency> dependencyInterface)
   {
      assertSame(Dependency.class, dependencyInterface);
      return DependencyImpl.class;
   }

   @Tested
   Class<?> resolveDAOInterfaces(Class<? extends BaseDAO> daoInterface)
   {
      assertSame(BaseDAO.class, daoInterface);
      return DAOImpl.class;
   }

   @Tested(fullyInitialized = true) TestedClass tested;

   @Test
   public void injectInterfaceImplementationsFromClassesReturnedFromTestedMethods()
   {
      assertTrue(tested.dependency instanceof DependencyImpl);
      assertTrue(tested.dao instanceof DAOImpl);
   }
}

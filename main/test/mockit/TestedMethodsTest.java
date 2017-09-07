/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import java.util.*;

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
   static class TestedClass { Dependency dependency; ConcreteDAO dao; AnotherDependency anotherDependency; Set<?> set; }

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

   @Tested
   Class<?> resolveAnythingElse(Class<?> anyInterface)
   {
      assertSame(AnotherDependency.class, anyInterface);
      return null;
   }

   @Tested(fullyInitialized = true) TestedClass tested;

   @Test
   public void injectInterfaceImplementationsFromClassesReturnedFromTestedMethods()
   {
      assertTrue(tested.dependency instanceof DependencyImpl);
      assertTrue(tested.dao instanceof DAOImpl);
      assertNull(tested.anotherDependency);
      assertNull(tested.set);
   }

   static final class DAO1 {}
   static final class DAO2 {}
   public interface BaseService {}
   static class BaseServiceImpl<D> { D dao; }
   public interface Service1 extends BaseService {}
   public interface Service2 extends BaseService {}
   static final class ConcreteService1 extends BaseServiceImpl<DAO1> implements Service1 {}
   static final class ConcreteService2 extends BaseServiceImpl<DAO2> implements Service2 { Service1 service1; }

   @Tested
   Class<? extends BaseServiceImpl<?>> resolveServiceInterfaces(Class<? extends BaseService> baseServiceType)
   {
      if (baseServiceType == Service1.class) return ConcreteService1.class;
      if (baseServiceType == Service2.class) return ConcreteService2.class;
      return null;
   }

   @Test
   public void createComplexObjectsWithGenericDependencies(@Tested(fullyInitialized = true) ConcreteService2 service2)
   {
      assertTrue(service2.dao instanceof DAO2);
      Service1 service1 = service2.service1;
      assertTrue(service1 instanceof ConcreteService1);
      assertTrue(((ConcreteService1) service1).dao instanceof DAO1);
   }
}

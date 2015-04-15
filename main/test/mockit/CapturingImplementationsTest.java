/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import java.lang.reflect.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.internal.*;

public final class CapturingImplementationsTest
{
   interface ServiceToBeStubbedOut { int doSomething(); }

   // Just to cause any implementing classes to be stubbed out.
   @Capturing ServiceToBeStubbedOut unused;

   static final class ServiceLocator
   {
      @SuppressWarnings("unused")
      static <S> S getInstance(Class<S> serviceInterface)
      {
         ServiceToBeStubbedOut service = new ServiceToBeStubbedOut() {
            @Override public int doSomething() { return 10; }
         };
         //noinspection unchecked
         return (S) service;
      }
   }

   @Test
   public void captureImplementationLoadedByServiceLocator()
   {
      ServiceToBeStubbedOut service = ServiceLocator.getInstance(ServiceToBeStubbedOut.class);
      assertEquals(0, service.doSomething());
   }

   public interface Service1 { int doSomething(); }
   static final class Service1Impl implements Service1 { @Override public int doSomething() { return 1; } }

   @Capturing Service1 mockService1;

   @Test
   public void captureImplementationUsingMockField()
   {
      Service1 service = new Service1Impl();

      new Expectations() {{
         mockService1.doSomething();
         returns(2, 3);
      }};

      assertEquals(2, service.doSomething());
      assertEquals(3, new Service1Impl().doSomething());
   }

   public interface Service2 { int doSomething(); }
   static final class Service2Impl implements Service2 { @Override public int doSomething() { return 1; } }

   @Test
   public void captureImplementationUsingMockParameter(@Capturing final Service2 mock)
   {
      Service2Impl service = new Service2Impl();

      new Expectations() {{
         mock.doSomething();
         returns(3, 2);
      }};

      assertEquals(3, service.doSomething());
      assertEquals(2, new Service2Impl().doSomething());
   }

   public abstract static class AbstractService { protected abstract boolean doSomething(); }

   static final class DefaultServiceImpl extends AbstractService
   {
      @Override
      protected boolean doSomething() { return true; }
   }

   @Test
   public void captureImplementationOfAbstractClass(@Capturing AbstractService mock)
   {
      assertFalse(new DefaultServiceImpl().doSomething());

      assertFalse(new AbstractService() {
         @Override
         protected boolean doSomething() { throw new RuntimeException(); }
      }.doSomething());
   }

   @Test
   public void captureGeneratedMockSubclass(@Capturing final AbstractService mock1, @Mocked final AbstractService mock2)
   {
      new Expectations() {{
         mock1.doSomething(); result = true;
         mock2.doSomething(); result = false;
      }};

      assertFalse(mock2.doSomething());
      assertTrue(mock1.doSomething());
      assertTrue(new DefaultServiceImpl().doSomething());
   }

   static final Class<? extends Service2> customLoadedClass = new ClassLoader() {
      @Override
      protected Class<? extends Service2> findClass(String name)
      {
         byte[] bytecode = ClassFile.readFromFile(name.replace('.', '/')).b;
         //noinspection unchecked
         return (Class<? extends Service2>) defineClass(name, bytecode, 0, bytecode.length);
      }
   }.findClass(Service2Impl.class.getName());

   final Service2 service2 = Deencapsulation.newInstance(customLoadedClass);

   @Test
   public void captureClassPreviouslyLoadedByClassLoaderOtherThanContext(@Capturing final Service2 mock)
   {
      new Expectations() {{
         mock.doSomething(); result = 15;
      }};

      assertEquals(15, service2.doSomething());
   }

   @BeforeClass
   public static void generateDynamicProxyClassBeforeCapturing()
   {
      proxyInstance = newProxyClassAndInstance(Service1.class, Serializable.class);
   }

   static Service1 newProxyClassAndInstance(Class<?>... interfacesToImplement)
   {
      ClassLoader loader = Service1.class.getClassLoader();

      return (Service1) Proxy.newProxyInstance(loader, interfacesToImplement, new InvocationHandler() {
         @Override
         public Object invoke(Object proxy, Method method, Object[] args)
         {
            fail("Should be mocked out");
            return null;
         }
      });
   }

   static Service1 proxyInstance;

   @Test
   public void captureDynamicallyGeneratedProxyClass() throws Exception
   {
      assertEquals(0, proxyInstance.doSomething());

      new Expectations() {{ mockService1.doSomething(); result = 123; }};

      assertEquals(123, proxyInstance.doSomething());

      Service1 anotherProxyInstance = newProxyClassAndInstance(Service1.class);
      assertEquals(123, anotherProxyInstance.doSomething());
   }
}

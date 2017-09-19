/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import java.lang.management.*;
import java.lang.reflect.*;

import javax.faces.event.*;
import javax.xml.parsers.*;

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

   Service2 service2;

   @Before
   public void instantiateCustomLoadedClass() throws Exception
   {
      Constructor<?> defaultConstructor = customLoadedClass.getDeclaredConstructors()[0];
      defaultConstructor.setAccessible(true);
      service2 = (Service2) defaultConstructor.newInstance();
   }

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

   interface Interface { void op(); }
   interface SubInterface extends Interface {}
   static class Implementation implements SubInterface { @Override public void op() { throw new RuntimeException(); } }

   @Test
   public void captureClassImplementingSubInterfaceOfCapturedInterface(@Capturing Interface base)
   {
      Interface impl = new Implementation();
      impl.op();
   }

   @Test
   public void captureClassesFromTheJavaManagementAPI(@Capturing ThreadMXBean anyThreadMXBean)
   {
      ThreadMXBean threadingBean = ManagementFactory.getThreadMXBean();
      int threadCount = threadingBean.getThreadCount();

      assertEquals(0, threadCount);
   }

   @Test
   public void captureClassesFromTheSAXParserAPI(@Capturing final SAXParser anyParser) throws Exception
   {
      new Expectations() {{ anyParser.isNamespaceAware(); result = true; }};

      SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
      boolean b = parser.isNamespaceAware();

      assertTrue(b);
   }

   interface Interface2 { int doSomething(); }
   interface SubInterface2 extends Interface2 {}
   static class ClassImplementingSubInterfaceAndExtendingUnrelatedBase extends Implementation implements SubInterface2 {
      @Override public int doSomething() { return 123; }
   }

   @Test
   public void captureClassWhichImplementsCapturedBaseInterfaceAndExtendsUnrelatedBase(@Capturing Interface2 captured)
   {
      int i = new ClassImplementingSubInterfaceAndExtendingUnrelatedBase().doSomething();

      assertEquals(0, i);
   }

   static class Base<T>
   {
      T doSomething() { return null; }
      void doSomething(T t) { System.out.println("test");}
   }

   static final class Impl extends Base<Integer>
   {
      @Override Integer doSomething() { return 1; }
      @Override void doSomething(Integer i) {}
   }

   @Test
   public void captureImplementationsOfGenericType(@Capturing final Base<Integer> anyInstance)
   {
      new Expectations() {{
         anyInstance.doSomething(); result = 2;
         anyInstance.doSomething(0);
      }};

      Base<Integer> impl = new Impl();
      int i = impl.doSomething();
      impl.doSomething(0);

      assertEquals(2, i);
   }

   static class Base2 { void base() {} }
   static class Sub extends Base2 {}
   static class Sub2 extends Sub { @Override void base() { throw new RuntimeException(); } }

   @Test
   public void verifyInvocationToMethodFromBaseClassOnCapturedSubclassOfIntermediateSubclass(@Capturing final Sub sub)
   {
      Sub impl = new Sub2();
      impl.base();

      new Verifications() {{
         sub.base();
      }};
   }

   public interface BaseItf { void base(); }
   public interface SubItf extends BaseItf {}

   @Test
   public void verifyInvocationToBaseInterfaceMethodOnCapturedImplementationOfSubInterface(@Capturing final SubItf sub)
   {
      SubItf impl = new SubItf() { @Override public void base() {} };
      impl.base();

      new Verifications() {{
         sub.base();
      }};
   }

   static final class MyActionListener implements ActionListener
   {
      @Override public void processAction(ActionEvent event) {}
      boolean doSomething() { return true; }
   }

   @Test
   public void captureUserDefinedClassImplementingExternalAPI(@Capturing ActionListener actionListener)
   {
      boolean notCaptured = new MyActionListener().doSomething();
      assertFalse(notCaptured);

      //noinspection UnnecessaryFullyQualifiedName
      new org.springframework.aop.aspectj.autoproxy.AspectJAwareAdvisorAutoProxyCreator();
   }
}

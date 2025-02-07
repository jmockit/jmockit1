package mockit;

import java.lang.management.*;
import java.lang.reflect.*;
import java.util.concurrent.*;

import javax.faces.event.*;
import javax.servlet.*;
import javax.xml.parsers.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.internal.*;

public final class CapturingImplementationsTest
{
   interface ServiceToBeStubbedOut { int doSomething(); }

   // Just to cause any implementing classes to be stubbed out.
   @Capturing ServiceToBeStubbedOut unused;

   static final class ServiceLocator {
      @SuppressWarnings("unused")
      static <S> S getInstance(Class<S> serviceInterface) {
         ServiceToBeStubbedOut service = new ServiceToBeStubbedOut() {
            @Override public int doSomething() { return 10; }
         };
         //noinspection unchecked
         return (S) service;
      }
   }

   @Test
   public void captureImplementationLoadedByServiceLocator() {
      ServiceToBeStubbedOut service = ServiceLocator.getInstance(ServiceToBeStubbedOut.class);
      assertEquals(0, service.doSomething());
   }

   public interface Service1 { int doSomething(); }
   static final class Service1Impl implements Service1 { @Override public int doSomething() { return 1; } }

   @Capturing Service1 mockService1;

   @Test
   public void captureImplementationUsingMockField() {
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
   public void captureImplementationUsingMockParameter(@Capturing final Service2 mock) {
      Service2Impl service = new Service2Impl();

      new Expectations() {{
         mock.doSomething();
         returns(3, 2);
      }};

      assertEquals(3, service.doSomething());
      assertEquals(2, new Service2Impl().doSomething());
   }

   public abstract static class AbstractService { protected abstract boolean doSomething(); }

   static final class DefaultServiceImpl extends AbstractService {
      @Override
      protected boolean doSomething() { return true; }
   }

   @Test
   public void captureImplementationOfAbstractClass(@Capturing AbstractService mock) {
      assertFalse(new DefaultServiceImpl().doSomething());

      assertFalse(new AbstractService() {
         @Override
         protected boolean doSomething() { throw new RuntimeException(); }
      }.doSomething());
   }

   static final Class<? extends Service2> customLoadedClass = new ClassLoader() {
      @Override
      protected Class<? extends Service2> findClass(String name) {
         byte[] bytecode = ClassFile.readBytesFromClassFile(name.replace('.', '/'));
         //noinspection unchecked
         return (Class<? extends Service2>) defineClass(name, bytecode, 0, bytecode.length);
      }
   }.findClass(Service2Impl.class.getName());

   Service2 service2;

   @Before
   public void instantiateCustomLoadedClass() throws Exception {
      Constructor<?> defaultConstructor = customLoadedClass.getDeclaredConstructors()[0];
      defaultConstructor.setAccessible(true);
      service2 = (Service2) defaultConstructor.newInstance();
   }

   @Test
   public void captureClassPreviouslyLoadedByClassLoaderOtherThanContext(@Capturing final Service2 mock) {
      new Expectations() {{ mock.doSomething(); result = 15; }};

      assertEquals(15, service2.doSomething());
   }

   public interface Service3 { int doSomething(); }
   static Service3 proxyInstance;

   @BeforeClass
   public static void generateDynamicProxyClass() {
      ClassLoader loader = Service3.class.getClassLoader();
      Class<?>[] interfaces = {Service3.class};
      InvocationHandler invocationHandler = new InvocationHandler() {
         @Override
         public Object invoke(Object proxy, Method method, Object[] args) {
            fail("Should be mocked out");
            return null;
         }
      };

      proxyInstance = (Service3) Proxy.newProxyInstance(loader, interfaces, invocationHandler);
   }

   @Test
   public void captureDynamicallyGeneratedProxyClass(@Capturing final Service3 mock) {
      new Expectations() {{ mock.doSomething(); result = 123; }};

      assertEquals(123, proxyInstance.doSomething());
   }

   interface Interface { void op(); }
   interface SubInterface extends Interface {}
   static class Implementation implements SubInterface { @Override public void op() { throw new RuntimeException(); } }

   @Test
   public void captureClassImplementingSubInterfaceOfCapturedInterface(@Capturing Interface base) {
      Interface impl = new Implementation();
      impl.op();
   }

   @Test
   public void captureClassesFromTheJavaManagementAPI(@Capturing ThreadMXBean anyThreadMXBean) {
      ThreadMXBean threadingBean = ManagementFactory.getThreadMXBean();
      int threadCount = threadingBean.getThreadCount();

      assertEquals(0, threadCount);
   }

   @Test
   public void captureClassesFromTheSAXParserAPI(@Capturing final SAXParser anyParser) throws Exception {
      new Expectations() {{ anyParser.isNamespaceAware(); result = true; }};

      SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
      boolean b = parser.isNamespaceAware();

      assertTrue(b);
   }

   @Test
   public void captureClassesFromTheJavaConcurrencyAPI(@Capturing ExecutorService anyExecutorService) {
      ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
      ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(2);
      ExecutorService cachedThreadPoolExecutor = Executors.newCachedThreadPool();
      ExecutorService scheduledThreadPoolExecutor = Executors.newScheduledThreadPool(3);

      // These calls would throw a NPE unless mocked.
      singleThreadExecutor.submit((Runnable) null);
      threadPoolExecutor.submit((Runnable) null);
      cachedThreadPoolExecutor.submit((Runnable) null);
      scheduledThreadPoolExecutor.submit((Callable<Object>) null);
   }

   interface Interface2 { int doSomething(); }
   interface SubInterface2 extends Interface2 {}
   static class ClassImplementingSubInterfaceAndExtendingUnrelatedBase extends Implementation implements SubInterface2 {
      @Override public int doSomething() { return 123; }
   }

   @Test
   public void captureClassWhichImplementsCapturedBaseInterfaceAndExtendsUnrelatedBase(@Capturing Interface2 captured) {
      int i = new ClassImplementingSubInterfaceAndExtendingUnrelatedBase().doSomething();

      assertEquals(0, i);
   }

   static class Base<T> {
      T doSomething() { return null; }
      void doSomething(T t) { System.out.println("test");}
      T doSomethingReturn(T t) { return t;}
   }

   static final class Impl extends Base<Integer> {
      @Override Integer doSomething() { return 1; }
      @Override void doSomething(Integer i) {}
      @Override Integer doSomethingReturn(Integer t) { return null;}
   }

   @Test
   public void captureImplementationsOfGenericType(@Capturing final Base<Integer> anyInstance) {
      new Expectations() {{
         anyInstance.doSomething(); result = 2;
         anyInstance.doSomethingReturn(0);
         anyInstance.doSomething(0);
      }};

      Base<Integer> impl = new Impl();
      int i = impl.doSomething();
      impl.doSomethingReturn(0);
      impl.doSomething(0);

      assertEquals(2, i);
   }

   static class Base2 { void base() {} }
   static class Sub extends Base2 {}
   static class Sub2 extends Sub { @Override void base() { throw new RuntimeException(); } }

   @Test
   public void verifyInvocationToMethodFromBaseClassOnCapturedSubclassOfIntermediateSubclass(@Capturing final Sub sub) {
      Sub impl = new Sub2();
      impl.base();

      new Verifications() {{
         sub.base();
      }};
   }

   public interface BaseItf { void base(); }
   public interface SubItf extends BaseItf {}

   @Test
   public void verifyInvocationToBaseInterfaceMethodOnCapturedImplementationOfSubInterface(@Capturing final SubItf sub) {
      SubItf impl = new SubItf() { @Override public void base() {} };
      impl.base();

      new Verifications() {{
         sub.base();
      }};
   }

   static final class MyActionListener implements ActionListener {
      @Override public void processAction(ActionEvent event) {}
      boolean doSomething() { return true; }
   }

   @Test
   public void captureUserDefinedClassImplementingExternalAPI(@Capturing ActionListener actionListener) {
      boolean notCaptured = new MyActionListener().doSomething();
      assertFalse(notCaptured);
   }

   @Test
   public void captureLibraryClassImplementingInterfaceFromAnotherLibrary(@Capturing final ServletContextListener mock) {
      //noinspection UnnecessaryFullyQualifiedName
      ServletContextListener contextListener = new org.springframework.web.util.WebAppRootListener();
      contextListener.contextInitialized(null);

      new Verifications() {{ mock.contextInitialized(null); }};
   }

   static class BaseGenericReturnTypes {
      Class<?> methodOne() {return null;}
      Class<?> methodTwo() {return null;}
   }
   static class SubGenericReturnTypes extends BaseGenericReturnTypes {}

   @Test
   public void captureMethodWithGenericReturnTypes(@Capturing final BaseGenericReturnTypes mock) {
      new Expectations () {{
         mock.methodOne();
         result = BaseGenericReturnTypes.class;
         times = 1;

         mock.methodTwo();
         result = SubGenericReturnTypes.class;
         times = 1;
      }};
      SubGenericReturnTypes subBaseGenericReturnTypes = new SubGenericReturnTypes();
      assertEquals(BaseGenericReturnTypes.class, subBaseGenericReturnTypes.methodOne());
      assertEquals(SubGenericReturnTypes.class, subBaseGenericReturnTypes.methodTwo());
   }

   static class BaseR {
     void foo() {};
     void bar() {};
   }

   static class SubR extends BaseR {}

   @Test
   public void captureR(@Capturing final BaseR mock) {
      new Expectations () {{
         mock.foo();
         times = 1;

         mock.bar();
         times = 1;
      }};
     SubR subR = new SubR();
     subR.foo();
     subR.bar();
   }

}
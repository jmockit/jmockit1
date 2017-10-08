/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import java.lang.management.*;
import java.net.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

import org.junit.*;
import org.junit.runners.*;

import static org.junit.Assert.*;

import mockit.internal.expectations.invocation.*;

@SuppressWarnings("ConstantConditions")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class CascadingParametersTest
{
   static class Foo
   {
      Bar getBar() { return null; }

      static Bar globalBar() { return null; }

      void doSomething(String s) { throw new RuntimeException(s); }
      int getIntValue() { return 1; }
      Boolean getBooleanValue() { return true; }
      final List<Integer> getList() { return null; }

      HashMap<?, ?> getMap() { return null; }
   }

   static class Bar
   {
      Bar() { throw new RuntimeException(); }
      int doSomething() { return 1; }
      Baz getBaz() { return null; }
      Baz getBaz(@SuppressWarnings("unused") int i) { return null; }
      AnEnum getEnum() { return null; }
      static String staticMethod() { return "notMocked"; }
   }

   static final class SubBar extends Bar {}

   public interface Baz
   {
      void runIt();
      Date getDate();
   }

   enum AnEnum { First, Second, Third }

   static Bar cascadedBar1;
   static Bar cascadedBar2;

   @Test
   public void cascadeOneLevelDuringReplay(@Mocked Foo foo)
   {
      cascadedBar1 = foo.getBar();
      assertEquals(0, cascadedBar1.doSomething());

      cascadedBar2 = Foo.globalBar();
      assertEquals(0, cascadedBar2.doSomething());

      Bar bar = foo.getBar();
      assertSame(cascadedBar1, bar);

      Bar globalBar = Foo.globalBar();
      assertSame(cascadedBar2, globalBar);
      assertNotSame(bar, globalBar);

      foo.doSomething("test");
      assertEquals(0, foo.getIntValue());
      assertFalse(foo.getBooleanValue());
      assertTrue(foo.getList().isEmpty());

      Map<?, ?> map = foo.getMap();
      assertNull(map);
   }

   @Test
   public void verifyThatPreviousCascadedInstancesHaveBeenDiscarded(@Mocked Foo foo)
   {
      Bar bar = foo.getBar();
      assertNotSame(cascadedBar1, bar);

      Bar globalBar = Foo.globalBar();
      assertNotSame(cascadedBar2, globalBar);
   }

   @Test
   public void verifyThatStaticMethodsAndConstructorsAreNotMockedWhenCascading(@Mocked Foo foo)
   {
      foo.getBar();
      
      assertEquals("notMocked", Bar.staticMethod());
      
      try {
         new Bar();
         fail();
      }
      catch (RuntimeException ignored) {}
   }

   @Test
   public void verifyThatStaticMethodsAndConstructorsAreMockedWhenCascadedMockIsMockedNormally(
      @Mocked Foo mockFoo, @Mocked Bar mockBar)
   {
      assertSame(mockBar, mockFoo.getBar());
      assertEquals(0, mockBar.doSomething());
      assertNull(Bar.staticMethod());
      new Bar();
   }

   @Test
   public void useAvailableMockedInstanceOfSubclassAsCascadedInstance(@Mocked Foo foo, @Mocked SubBar bar)
   {
      Bar cascadedBar = foo.getBar();

      assertSame(bar, cascadedBar);
   }

   @Test
   public void replaceCascadedInstanceWithFirstOneOfTwoInjectableInstances(
      @Mocked final Foo foo, @Injectable final Bar bar1, @Injectable Bar bar2)
   {
      new Expectations() {{ foo.getBar(); result = bar1; }};

      Bar cascadedBar = foo.getBar();

      assertSame(bar1, cascadedBar);
      assertEquals(0, bar1.doSomething());
      assertEquals(0, bar2.doSomething());
   }

   @Test
   public void cascadeOneLevelDuringRecord(@Mocked final Foo mockFoo)
   {
      final List<Integer> list = Arrays.asList(1, 2, 3);

      new Expectations() {{
         mockFoo.doSomething(anyString); minTimes = 2;
         mockFoo.getBar().doSomething(); result = 2;
         Foo.globalBar().doSomething(); result = 3;
         mockFoo.getBooleanValue(); result = true;
         mockFoo.getIntValue(); result = -1;
         mockFoo.getList(); result = list;
      }};

      Foo foo = new Foo();
      foo.doSomething("1");
      assertEquals(2, foo.getBar().doSomething());
      foo.doSomething("2");
      assertEquals(3, Foo.globalBar().doSomething());
      assertTrue(foo.getBooleanValue());
      assertEquals(-1, foo.getIntValue());
      assertSame(list, foo.getList());
   }

   @Test
   public void cascadeOneLevelDuringVerify(@Mocked final Foo foo)
   {
      Bar bar = foo.getBar();
      bar.doSomething();
      bar.doSomething();

      Foo.globalBar().doSomething();

      assertEquals(0, foo.getIntValue());
      assertFalse(foo.getBooleanValue());

      assertTrue(foo.getList().isEmpty());

      new Verifications() {{
         foo.getBar().doSomething(); minTimes = 2;
         Foo.globalBar().doSomething(); times = 1;
      }};

      new VerificationsInOrder() {{
         foo.getIntValue();
         foo.getBooleanValue();
      }};
   }

   @Test
   public void cascadeTwoLevelsDuringReplay(@Mocked Foo foo)
   {
      foo.getBar().getBaz().runIt();
   }

   @Test
   public void cascadeTwoLevelsDuringRecord(@Mocked final Foo mockFoo)
   {
      new Expectations() {{
         mockFoo.getBar().doSomething(); result = 1;
         Foo.globalBar().doSomething(); result = 2;

         mockFoo.getBar().getBaz().runIt(); times = 2;
      }};

      Foo foo = new Foo();
      assertEquals(1, foo.getBar().doSomething());
      assertEquals(2, Foo.globalBar().doSomething());

      Baz baz = foo.getBar().getBaz();
      baz.runIt();
      baz.runIt();
   }

   @Test
   public void cascadeOneLevelAndVerifyInvocationOnLastMockOnly(@Mocked Foo foo, @Injectable final Bar bar)
   {
      Bar fooBar = foo.getBar();
      assertSame(bar, fooBar);
      fooBar.doSomething();

      new Verifications() {{ bar.doSomething(); }};
   }

   @Test
   public void cascadeTwoLevelsWithInvocationRecordedOnLastMockOnly(@Mocked Foo foo, @Mocked final Baz baz)
   {
      new Expectations() {{
         baz.runIt(); times = 1;
      }};

      Baz cascadedBaz = foo.getBar().getBaz();
      cascadedBaz.runIt();
   }

   @Test
   public void cascadeTwoLevelsAndVerifyInvocationOnLastMockOnly(@Mocked Foo foo, @Mocked final Baz baz)
   {
      Baz cascadedBaz = foo.getBar().getBaz();
      assertSame(baz, cascadedBaz);
      cascadedBaz.runIt();

      new Verifications() {{ baz.runIt(); }};
   }

   // Tests using the java.lang.Process and java.lang.ProcessBuilder classes //////////////////////////////////////////

   @Test
   public void cascadeOnJREClasses(@Mocked final ProcessBuilder pb) throws Exception
   {
      new Expectations() {{
         ProcessBuilder sameBuilder = pb.directory((File) any);
         assertSame(sameBuilder, pb);

         Process process = sameBuilder.start();
         process.getOutputStream().write(5);
         process.exitValue(); result = 1;
      }};

      Process process = new ProcessBuilder("test").directory(new File("myDir")).start();
      process.getOutputStream().write(5);
      process.getOutputStream().flush();
      assertEquals(1, process.exitValue());
   }

   @Test
   public void returnSameMockedInstanceThroughCascadingEvenWithMultipleCandidatesAvailable(
      @Injectable ProcessBuilder pb1, @Injectable ProcessBuilder pb2)
   {
      assertSame(pb1, pb1.command("a"));
      assertSame(pb2, pb2.command("b"));
   }

   @Test
   public void createOSProcessToCopyTempFiles(@Mocked final ProcessBuilder pb) throws Exception
   {
      // Code under test creates a new process to execute an OS-specific command.
      String cmdLine = "copy /Y *.txt D:\\TEMP";
      File wrkDir = new File("C:\\TEMP");
      Process copy = new ProcessBuilder().command(cmdLine).directory(wrkDir).start();
      int exit = copy.waitFor();

      if (exit != 0) {
         throw new RuntimeException("Process execution failed");
      }

      // Verify the desired process was created with the correct command.
      new Verifications() {{ pb.command(withSubstring("copy")).start(); }};
   }

   // Tests using java.net classes ////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void recordAndVerifyExpectationsOnCascadedMocks(
      @Mocked Socket anySocket, @Mocked final SocketChannel cascadedChannel, @Mocked InetSocketAddress inetAddr)
      throws Exception
   {
      Socket sk = new Socket();
      SocketChannel ch = sk.getChannel();

      if (!ch.isConnected()) {
         SocketAddress sa = new InetSocketAddress("remoteHost", 123);
         ch.connect(sa);
      }

      InetAddress adr1 = sk.getInetAddress();
      InetAddress adr2 = sk.getLocalAddress();
      assertNotSame(adr1, adr2);

      new Verifications() {{ cascadedChannel.connect((SocketAddress) withNotNull()); }};
   }

   static final class SocketFactory
   {
      public Socket createSocket() { return new Socket(); }

      public Socket createSocket(String host, int port) throws IOException
      {
         return new Socket(host, port);
      }
   }

   @Test
   public void mockDynamicallyAClassToBeLaterMockedThroughCascading()
   {
      new Expectations(Socket.class) {};
   }

   @Test
   public void cascadeOneLevelWithArgumentMatchers(@Mocked final SocketFactory sf) throws Exception
   {
      new Expectations() {{
         sf.createSocket(anyString, 80); result = null;
      }};

      assertNull(sf.createSocket("expected", 80));
      assertNotNull(sf.createSocket("unexpected", 8080));
   }

   @Test
   public void recordAndVerifyOneLevelDeep(@Mocked final SocketFactory sf) throws Exception
   {
      final OutputStream out = new ByteArrayOutputStream();

      new Expectations() {{
         sf.createSocket().getOutputStream(); result = out;
      }};

      assertSame(out, sf.createSocket().getOutputStream());
   }

   @Test
   public void recordAndVerifyOnTwoCascadingMocksOfTheSameType(
      @Mocked final SocketFactory sf1, @Mocked final SocketFactory sf2) throws Exception
   {
      final OutputStream out1 = new ByteArrayOutputStream();
      final OutputStream out2 = new ByteArrayOutputStream();

      new Expectations() {{
         sf1.createSocket().getOutputStream(); result = out1;
         sf2.createSocket().getOutputStream(); result = out2;
      }};

      assertSame(out1, sf1.createSocket().getOutputStream());
      assertSame(out2, sf2.createSocket().getOutputStream());

      new FullVerificationsInOrder() {{
         sf1.createSocket().getOutputStream();
         sf2.createSocket().getOutputStream();
      }};
   }

   @Test
   public void recordAndVerifySameInvocationOnMocksReturnedFromInvocationsWithDifferentArguments(
      @Mocked final SocketFactory sf) throws Exception
   {
      new Expectations() {{
         sf.createSocket().getPort(); result = 1;
         sf.createSocket("first", 80).getPort(); result = 2;
         sf.createSocket("second", 80).getPort(); result = 3;
         sf.createSocket(anyString, 81).getPort(); result = 4;
      }};

      assertEquals(1, sf.createSocket().getPort());
      assertEquals(2, sf.createSocket("first", 80).getPort());
      assertEquals(3, sf.createSocket("second", 80).getPort());
      assertEquals(4, sf.createSocket("third", 81).getPort());

      new VerificationsInOrder() {{
         sf.createSocket().getPort(); times = 1;
         sf.createSocket("first", 80).getPort();
         sf.createSocket("second", 80).getPort();
         sf.createSocket(anyString, 81).getPort(); maxTimes = 1;
         sf.createSocket("fourth", -1); times = 0;
      }};
   }

   @Test
   public void cascadeOnInheritedMethod(@Mocked SocketChannel sc)
   {
      assertNotNull(sc.provider());
   }

   @Test
   public void recordAndVerifyWithMixedCascadeLevels(@Mocked final SocketFactory sf) throws Exception
   {
      new Expectations() {{
         sf.createSocket("first", 80).getKeepAlive(); result = true;
         sf.createSocket("second", anyInt).getChannel().close(); times = 1;
      }};

      sf.createSocket("second", 80).getChannel().close();
      assertTrue(sf.createSocket("first", 80).getKeepAlive());
      sf.createSocket("first", 8080).getChannel().provider().openPipe();

      new Verifications() {{
         sf.createSocket("first", 8080).getChannel().provider().openPipe();
      }};
   }

   // Cascading other Java SE types ///////////////////////////////////////////////////////////////////////////////////

   static class SomeClass { Future<Foo> doSomething() { return null; } }

   @Test
   public void cascadeAFuture(@Mocked SomeClass mock) throws Exception
   {
      Future<Foo> f = mock.doSomething();
      Foo foo = f.get();

      assertNotNull(foo);
   }

   // Other tests /////////////////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void recordExpectationOnCascadedMock(@Mocked Foo foo, @Mocked final Bar mockBar)
   {
      new Expectations() {{
         mockBar.doSomething(); times = 1; result = 123;
      }};

      Bar bar = foo.getBar();
      assertEquals(123, bar.doSomething());
   }

   @Test
   public void overrideTwoCascadedMocksOfTheSameType(
      @Mocked final Foo foo1, @Mocked final Foo foo2, @Mocked final Bar mockBar1, @Mocked final Bar mockBar2)
   {
      new Expectations() {{
         foo1.getBar(); result = mockBar1;
         foo2.getBar(); result = mockBar2;
         mockBar1.doSomething();
         mockBar2.doSomething();
      }};

      Bar bar1 = foo1.getBar();
      Bar bar2 = foo2.getBar();
      bar1.doSomething();
      bar2.doSomething();
   }

   @Test(expected = MissingInvocation.class)
   public void overrideTwoCascadedMocksOfTheSameTypeButReplayInDifferentOrder(
      @Mocked final Foo foo1, @Mocked final Foo foo2, @Injectable final Bar mockBar1, @Mocked final Bar mockBar2)
   {
      new Expectations() {{
         foo1.getBar(); result = mockBar1;
         foo2.getBar(); result = mockBar2;
      }};

      Bar bar1 = foo1.getBar();
      Bar bar2 = foo2.getBar();
      bar2.doSomething();
      bar1.doSomething();

      new FullVerificationsInOrder() {{
         mockBar1.doSomething();
         mockBar2.doSomething();
      }};
   }

   @Test
   public void cascadedEnum(@Mocked final Foo mock)
   {
      new Expectations() {{
         mock.getBar().getEnum(); result = AnEnum.Second;
      }};

      assertEquals(AnEnum.Second, mock.getBar().getEnum());
   }

   @Test
   public void cascadedEnumReturningConsecutiveValuesThroughResultField(@Mocked final Foo mock)
   {
      new Expectations() {{
         mock.getBar().getEnum();
         result = AnEnum.First;
         result = AnEnum.Second;
         result = AnEnum.Third;
      }};

      assertSame(AnEnum.First, mock.getBar().getEnum());
      assertSame(AnEnum.Second, mock.getBar().getEnum());
      assertSame(AnEnum.Third, mock.getBar().getEnum());
   }

   @Test
   public void cascadedEnumReturningConsecutiveValuesThroughReturnsMethod(@Mocked final Foo mock)
   {
      new Expectations() {{
         mock.getBar().getEnum();
         returns(AnEnum.First, AnEnum.Second, AnEnum.Third);
      }};

      assertSame(AnEnum.First, mock.getBar().getEnum());
      assertSame(AnEnum.Second, mock.getBar().getEnum());
      assertSame(AnEnum.Third, mock.getBar().getEnum());
   }

   @Test
   public void overrideLastCascadedObjectWithNonMockedInstance(@Mocked final Foo foo)
   {
      final Date newDate = new Date(123);
      assertEquals(123, newDate.getTime());

      new Expectations() {{
         foo.getBar().getBaz().getDate();
         result = newDate;
      }};

      assertSame(newDate, new Foo().getBar().getBaz().getDate());
      assertEquals(123, newDate.getTime());
   }

   @Test
   public void returnDeclaredMockedInstanceFromMultiLevelCascading(@Mocked Date mockedDate, @Mocked Foo foo)
   {
      Date newDate = new Date(123);
      assertEquals(0, newDate.getTime());

      Date cascadedDate = new Foo().getBar().getBaz().getDate();

      assertSame(mockedDate, cascadedDate);
      assertEquals(0, newDate.getTime());
      assertEquals(0, mockedDate.getTime());
   }

   @Test
   public void returnInjectableMockInstanceFromMultiLevelCascading(@Injectable Date mockDate, @Mocked Foo foo)
   {
      Date newDate = new Date(123);
      assertEquals(123, newDate.getTime());

      Date cascadedDate = new Foo().getBar().getBaz().getDate();

      assertSame(mockDate, cascadedDate);
      assertEquals(123, newDate.getTime());
      assertEquals(0, mockDate.getTime());
   }

   static class Factory { static Factory create() { return null; } }
   static class Client { OtherClient getOtherClient() { return null; } }
   static class OtherClient { static final Factory F = Factory.create(); }

   @Test
   public void cascadeDuringStaticInitializationOfCascadingClass(@Mocked Factory mock1, @Mocked Client mock2)
   {
      assertNotNull(mock2.getOtherClient());
      assertNotNull(OtherClient.F);
   }

   public interface LevelZero { Runnable getFoo(); }
   public interface LevelOne extends LevelZero {}
   public interface LevelTwo extends LevelOne {}

   @Test
   public void createCascadedMockFromMethodDefinedTwoLevelsUpAnInterfaceHierarchy(@Mocked LevelTwo mock)
   {
      assertNotNull(mock.getFoo());
   }

   public abstract class AbstractClass implements LevelZero {}

   @Test
   public void cascadeTypeReturnedFromInterfaceImplementedByAbstractClass(@Mocked AbstractClass mock)
   {
      Runnable foo = mock.getFoo();
      assertNotNull(foo);
   }

   @Test
   public void produceDifferentCascadedInstancesOfSameInterfaceFromDifferentInvocations(@Mocked Bar bar)
   {
      Baz cascaded1 = bar.getBaz(1);
      Baz cascaded2 = bar.getBaz(2);
      Baz cascaded3 = bar.getBaz(1);

      assertSame(cascaded1, cascaded3);
      assertNotSame(cascaded1, cascaded2);
   }

   @Test
   public void cascadeFromJavaManagementAPI(@Mocked ManagementFactory mngmntFactory)
   {
      CompilationMXBean compilation = ManagementFactory.getCompilationMXBean();

      assertNotNull(compilation);
      assertNull(compilation.getName());
   }

   public interface AnInterface { NonPublicTestedClass getPackagePrivateClass(); }

   @Test
   public void cascadeFromMethodInPublicInterfaceReturningPackagePrivateType(@Mocked AnInterface mock)
   {
      NonPublicTestedClass ret = mock.getPackagePrivateClass();

      assertNull(ret);
   }

   public static final class CustomException extends Throwable  {}
   static class AClass { CustomException getException() { return new CustomException(); } }

   @Test
   public void cascadeFromMethodReturningAThrowableSubclass(@Mocked AClass mock)
   {
      CustomException t = mock.getException();

      assertNull(t);
   }

   static class First { <T extends Second> T getSecond(@SuppressWarnings("unused") Class<T> aClass) { return null; } }
   static class Second { Runnable getSomething() { return null; }}

   @Test
   public void cascadeFromMethodReturningTypeProvidedByClassParameterThenFromCascadedInstance(@Mocked First first)
   {
      Second second = first.getSecond(Second.class);
      Runnable runnable = second.getSomething();

      assertNotNull(runnable);
   }
}
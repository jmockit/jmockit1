package mockit;

import java.nio.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class MockedParametersWithCapturingTest
{
   public interface Service {
      int doSomething();
      void doSomethingElse(int i);
   }

   static final class ServiceImpl implements Service {
      final String str;

      ServiceImpl(String str) { this.str = str; }

      @Override public int doSomething() { return 1; }
      @Override public void doSomethingElse(int i) { throw new IllegalMonitorStateException(); }
   }

   static class BaseClass {
      final String str;
      BaseClass() { str = ""; }
      BaseClass(String str) { this.str = str; }
      String getStr() { return str; }
      void doSomething() { throw new IllegalStateException("Invalid state"); }
   }

   static class DerivedClass extends BaseClass {
      DerivedClass() {}
      DerivedClass(String str) { super(str); }
      @Override String getStr() { return super.getStr().toUpperCase(); }
   }

   @Test
   public void captureDerivedClass(@Capturing BaseClass service) {
      assertNull(new DerivedClass("test").str);
      assertNull(new DerivedClass() {}.str);
   }

   @Test
   public void captureImplementationsOfDifferentInterfaces(@Capturing Runnable mock1, @Capturing Readable mock2) throws Exception {
      Runnable runnable = new Runnable() {
         @Override
         public void run() { throw new RuntimeException("run"); }
      };
      runnable.run();

      Readable readable = new Readable() {
         @Override
         public int read(CharBuffer cb) { throw new RuntimeException("read"); }
      };
      readable.read(CharBuffer.wrap("test"));
   }

   @Test
   public void captureImplementationsOfAnInterface(@Capturing final Service service) {
      Service impl1 = new ServiceImpl("test1");
      impl1.doSomethingElse(1);

      Service impl2 = new Service() {
         @Override public int doSomething() { return 2; }
         @Override public void doSomethingElse(int i) { throw new IllegalStateException("2"); }
      };
      impl2.doSomethingElse(2);
   }

   @Test
   public void captureSubclassesOfABaseClass(@Capturing final BaseClass base) {
      BaseClass impl1 = new DerivedClass("test1");
      impl1.doSomething();

      BaseClass impl2 = new BaseClass("test2") {
         @Override void doSomething() { throw new IllegalStateException("2"); }
      };
      impl2.doSomething();

      final class DerivedClass2 extends DerivedClass {
         DerivedClass2() { super("DeRiVed"); }
         @Override String getStr() { return super.getStr().toLowerCase(); }
      }
      DerivedClass2 impl3 = new DerivedClass2();
      impl3.doSomething();
   }

   public interface IBase { int doSomething(); }
   public interface ISub extends IBase {}

   @Test
   public void recordCallToBaseInterfaceMethodOnCaptureSubInterfaceImplementation(@Capturing final ISub mock) {
      new Expectations() {{ mock.doSomething(); result = 123; }};

      ISub impl = new ISub() { @Override public int doSomething() { return -1; } };
      int i = impl.doSomething();

      assertEquals(123, i);
   }
}
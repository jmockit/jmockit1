package mockit;

import static org.junit.Assert.*;
import org.junit.*;

public final class ClassInitializationTest
{
   static class ClassWithStaticInitializer {
      static final Object CONSTANT = "not a compile-time constant";
      static String variable;
      static { variable = doSomething(); }
      static String doSomething() { return "real value"; }
   }

   @Test
   public void mockClassWithStaticInitializer(@Mocked ClassWithStaticInitializer mocked) {
      //noinspection ConstantJUnitAssertArgument
      assertNotNull(ClassWithStaticInitializer.CONSTANT);
      assertNull(ClassWithStaticInitializer.doSomething());
      assertEquals("real value", ClassWithStaticInitializer.variable);
   }

   static class ClassWhichCallsStaticMethodFromInitializer {
      static {
         String s = someValue();
         s.length();
      }

      static String someValue() { return "some value"; }
   }

   @Test
   public void mockUninitializedClass(@Mocked ClassWhichCallsStaticMethodFromInitializer unused) {
      assertNull(ClassWhichCallsStaticMethodFromInitializer.someValue());
   }

   public interface BaseType { String someValue(); }
   static final class NestedImplementationClass implements BaseType {
      static { new NestedImplementationClass().someValue().length(); }
      @Override public String someValue() { return "some value"; }
   }

   @Before
   public void loadNestedImplementationClass() {
      // Ensure the class gets loaded, but not initialized, before it gets mocked.
      // The HotSpot VM would (for some reason) already have loaded it, but the J9 VM would not.
      NestedImplementationClass.class.getName();
   }

   @Test
   public void mockUninitializedImplementationClass(@Capturing BaseType mockBase) {
      BaseType obj = new NestedImplementationClass();

      assertNull(obj.someValue());
   }

   static class Dependency { static Dependency create() { return null; } }
   static class Dependent {
      static final Dependency DEPENDENCY = Dependency.create();
      static { DEPENDENCY.toString(); }
   }
   static class AnotherDependent {
      static final Dependency DEPENDENCY = Dependency.create();
      static { DEPENDENCY.toString(); }
   }

   @Mocked Dependency dependency;
   @Mocked Dependent dependent;

   @Test
   public void mockAnotherDependentClass(@Mocked AnotherDependent anotherDependent) {
      assertNotNull(Dependent.DEPENDENCY);
      assertNotNull(AnotherDependent.DEPENDENCY);
   }

   public interface BaseInterface { Object DO_NOT_REMOVE = "Testing"; }
   public interface SubInterface extends BaseInterface {}
   @Mocked SubInterface mock;

   @Test
   public void verifyClassInitializerForMockedBaseInterface() {
      assertNotNull(mock);
      assertEquals("Testing", BaseInterface.DO_NOT_REMOVE);
   }

   static final class ClassWhichCallsMethodOnItselfFromInitializer {
      static final Integer value = value();
      static Integer value() { return null; }
   }

   @Test
   public void mockClassWhichCallsMethodOnItselfFromInitializer(@Mocked ClassWhichCallsMethodOnItselfFromInitializer unused) {
      assertNotNull(ClassWhichCallsMethodOnItselfFromInitializer.value());
      assertNull(ClassWhichCallsMethodOnItselfFromInitializer.value);
   }

   interface InterfaceWithStaticInitializer { Object CONSTANT = "test"; }
   @SuppressWarnings({"AbstractClassWithoutAbstractMethods", "StaticInheritance"})
   public abstract static class AbstractImpl implements InterfaceWithStaticInitializer {}

   @Test // failed on JDK 9+ only
   public void mockAbstractClassImplementingInterfaceWithStaticInitializer(@Mocked AbstractImpl mock2) {
      assertEquals("test", mock2.CONSTANT);
   }
}
package mockit;

import java.lang.annotation.*;

import javax.persistence.*;
import javax.sql.*;

import org.junit.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

@FixMethodOrder(NAME_ASCENDING)
public final class TestedClassWithFullDITest
{
   public static class TestedClass {
      Runnable dependencyToBeMocked;
      FirstLevelDependency dependency2;
      FirstLevelDependency dependency3;
      CommonDependency commonDependency;
      String name;
      StringBuilder description;
      final Integer number = null;
      boolean flag = true;
      Thread.State threadState;
      AnotherTestedClass subObj;
      YetAnotherTestedClass subObj2;
      volatile CommonDependency notToBeInjected;
   }

   public static class FirstLevelDependency {
      String firstLevelId;
      SecondLevelDependency dependency;
      CommonDependency commonDependency;
      Runnable dependencyToBeMocked;
   }

   public static class SecondLevelDependency { CommonDependency commonDependency; }
   public static class CommonDependency {}

   @Tested(fullyInitialized = true)
   @Retention(RetentionPolicy.RUNTIME)
   @Target(ElementType.FIELD)
   public @interface IntegrationTested {}

   public static class YetAnotherTestedClass {}
   @IntegrationTested YetAnotherTestedClass tested3;

   @IntegrationTested TestedClass tested;
   @Injectable Runnable mockedDependency;

   @Test
   public void useFullyInitializedTestedObjectWithNoInjectableForFirstLevelDependency() {
      assertNull(tested.name);
      assertSame(tested.commonDependency, tested.dependency2.dependency.commonDependency);
      assertNull(tested.notToBeInjected);
   }

   @Test
   public void useFullyInitializedTestedObjectWithValueForFirstLevelDependency(@Injectable("test") String id) {
      assertEquals("test", tested.name);
      assertNull(tested.description);
      assertNull(tested.number);
      assertTrue(tested.flag);
      assertNull(tested.threadState);
      assertSame(mockedDependency, tested.dependencyToBeMocked);
      assertNotNull(tested.dependency2);
      assertEquals("test", tested.dependency2.firstLevelId);
      assertSame(tested.dependency2, tested.dependency3);
      assertNotNull(tested.commonDependency);
      assertNotNull(tested.dependency2.dependency);
      assertSame(tested.dependency2.dependency, tested.dependency3.dependency);
      assertSame(tested.commonDependency, tested.dependency2.commonDependency);
      assertSame(tested.commonDependency, tested.dependency3.commonDependency);
      assertSame(tested.commonDependency, tested.dependency2.dependency.commonDependency);
      assertSame(mockedDependency, tested.dependency2.dependencyToBeMocked);
      assertSame(mockedDependency, tested.dependency3.dependencyToBeMocked);
   }

   public static class AnotherTestedClass { YetAnotherTestedClass subObj; }
   @IntegrationTested AnotherTestedClass tested2;

   @Test
   public void verifyOtherTestedObjectsGetInjectedIntoFirstOne() {
      assertSame(tested2, tested.subObj);
      assertSame(tested3, tested.subObj2);
      assertSame(tested3, tested.subObj.subObj);
   }

   @Tested DependencyImpl concreteDependency;
   @IntegrationTested ClassWithDependencyOfAbstractType tested4;

   public interface Dependency {}
   static class DependencyImpl implements Dependency {}
   static class ClassWithDependencyOfAbstractType { Dependency dependency; }

   @Test
   public void useTestedObjectOfSubtypeForAbstractDependencyTypeInAnotherTestedObject() {
      assertSame(concreteDependency, tested4.dependency);
   }

   static class A { B b1; }
   static class B { B b2; }
   @Tested(fullyInitialized = true) A a;

   @Test
   public void instantiateClassDependentOnAnotherHavingFieldOfItsOwnType() {
      B b1 = a.b1;
      assertNotNull(b1);

      B b2 = b1.b2;
      assertNotNull(b2);
      assertSame(b1, b2);
   }

   @Entity static class Person {}
   static class ClassWithJPAEntityField { Person person; }

   @Test
   public void instantiateClassWithJPAEntityField(@Tested(fullyInitialized = true) ClassWithJPAEntityField tested5) {
      assertNull(tested5.person);
   }

   static class ClassWithDataSourceField { DataSource ds; }

   @Test
   public void instantiateClassWithNonAnnotatedDataSourceField(@Tested(fullyInitialized = true) ClassWithDataSourceField tested5) {
      assertNull(tested5.ds);
   }

   static class ClassWithJPAFields { EntityManagerFactory emFactory; EntityManager em; }

   @Test
   public void instantiateClassWithNonAnnotatedJPAFields(@Tested(fullyInitialized = true) ClassWithJPAFields tested6) {
      // If an EntityManagerFactory was created for a previous test, then it got stored in the global dependency cache, which lasts
      // until the end of the test run; therefore, the assertion needs to allow for that.
      assertTrue(tested6.emFactory == null || tested6.emFactory.getClass().getName().contains("FakeEntityManagerFactory"));
      assertNull(tested6.em);
   }

   static class ClassWithUnsatisfiableConstructor { ClassWithUnsatisfiableConstructor(@SuppressWarnings("unused") int someValue) {} }
   static class ClassWithFieldToInject { ClassWithUnsatisfiableConstructor dependency; }

   @Test
   public void instantiateClassWithFieldToInjectWhoseTypeCannotBeInstantiated(@Tested(fullyInitialized = true) ClassWithFieldToInject cut) {
      assertNotNull(cut);
      assertNull(cut.dependency);
   }

   static interface InterfaceDependency { }
   static class ClassWithInterfaceInConstructor { ClassWithInterfaceInConstructor(@SuppressWarnings("unused") InterfaceDependency someValue) {} }

   @Test
   public void instantiateClassWithInterfaceInConstructor(@Tested(fullyInitialized = true) ClassWithInterfaceInConstructor cut) {
      assertNotNull(cut);
   }

}
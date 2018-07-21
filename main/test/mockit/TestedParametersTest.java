package mockit;

import java.lang.annotation.*;
import javax.inject.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class TestedParametersTest
{
   static class TestedClass {
      final int i;
      final Collaborator collaborator;
      Dependency dependency;

      TestedClass() { i = -1; collaborator = null; }
      TestedClass(int i, Collaborator collaborator) { this.i = i; this.collaborator = collaborator; }
   }

   static class Dependency {}
   static final class Collaborator {}

   @Test
   public void createTestedObjectForTestMethodParameter(@Tested Dependency dep) {
      assertNotNull(dep);
   }

   @Tested TestedClass tested1;
   @Tested(fullyInitialized = true) TestedClass tested2;

   @Test
   public void injectTestedObjectFromTestMethodParameterIntoFullyInitializedTestedObject(@Tested Dependency dep) {
      assertEquals(-1, tested2.i);
      assertNull(tested2.collaborator);
      assertSame(dep, tested2.dependency);
   }

   @Test
   public void injectTestedParametersIntoTestedFieldsUsingConstructor(
      @Tested("123") int i, @Tested Collaborator collaborator
   ) {
      assertEquals(123, i);
      assertNotNull(collaborator);

      assertEquals(123, tested1.i);
      assertSame(collaborator, tested1.collaborator);
      assertNull(tested1.dependency);

      assertEquals(123, tested2.i);
      assertSame(collaborator, tested2.collaborator);
      assertNotNull(tested2.dependency);
   }

   static class TestedClass2 { CharSequence text; Number n; Comparable<Float> cmp; }

   @Test
   public void injectTestedParametersIntoTestedFieldsOfSupertypes(
      @Tested("test") String s, @Tested("123") Integer n, @Tested("5.2") Float cmp,
      @Tested(fullyInitialized = true) TestedClass2 tested
   ) {
      assertEquals("test", tested.text);
      assertEquals(123, tested.n.intValue());
      assertEquals(5.2F, tested.cmp);
   }

   static class TestedClass3 { String text; Number number; }

   @Test
   public void injectTestedParametersWithValuesIntoFieldsOfRegularTestedObject(
      @Tested("test") String s, @Tested("123") Integer n, @Tested TestedClass3 tested
   ) {
      assertEquals("test", tested.text);
      assertEquals(123, tested.number);
   }

   static class TestedClass4 {
      final String text;
      final Number number;
      TestedClass4(String text, Number number) { this.text = text; this.number = number; }
   }

   @Test
   public void injectTestedParameterWithValueIntoRegularTestedObjectThroughConstructorParameter(
      @Tested("test") String text, @Tested("1.23") Double number, @Tested TestedClass4 tested
   ) {
      assertEquals("test", tested.text);
      assertEquals(1.23, tested.number);
   }

   static class AnotherDependency {}
   static class TestedClassWithDIAnnotatedField { @Inject AnotherDependency dep; }

   @Injectable AnotherDependency anotherDep;

   @Test
   public void injectInjectableFieldIntoTestedParameter(@Tested TestedClassWithDIAnnotatedField tested) {
      assertSame(anotherDep, tested.dep);
   }

   @Target(PARAMETER) @Retention(RUNTIME) @Tested public @interface InjectedDependency {}

   @Test
   public void injectParameterUsingTestedAsMetaAnnotation(@InjectedDependency Collaborator col) {
      assertNotNull(col);
   }
}

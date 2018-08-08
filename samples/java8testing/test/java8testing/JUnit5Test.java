package java8testing;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import mockit.*;

final class JUnit5Test
{
   @Tested(availableDuringSetup = true) TestUtils utils;
   @Tested BusinessService cut;
   @Injectable Collaborator collaborator;

   @BeforeEach
   void checkMockAndTestedFields() {
      assertNotNull(utils);
      assertNotNull(collaborator);
      assertNull(cut);
   }

   @AfterEach
   void checkMockAndTestedFieldsAgain() {
      assertNotNull(utils);
      assertNotNull(collaborator);
      assertNull(cut);
   }

   @Test
   void withParameterProvidedByJUnit(TestInfo testInfo) {
      assertNotNull(testInfo);
   }

   @Test
   void withMockParameters(@Mocked Runnable mock, @Injectable("test") String text) {
      assertNotNull(mock);
      assertEquals("test", text);
      assertNotNull(collaborator);
      assertSame(collaborator, cut.getCollaborator());
   }

   @Target(PARAMETER) @Retention(RUNTIME) @Tested public @interface InjectedDependency {}

   @Test
   public void injectParameterUsingTestedAsMetaAnnotation(@InjectedDependency BusinessService col) {
      assertNotNull(col);
   }

   @Test
   void recordExpectationOnCollaborator() {
      new Expectations() {{ collaborator.doSomething(anyInt); result = "Test"; }};

      String result = cut.performBusinessOperation(123);

      assertEquals("Test", result);
   }

   @Nested
   final class InnerTest {
      @BeforeEach
      void setUp() {
         assertNotNull(utils);
         assertNotNull(collaborator);
         assertNull(cut);
      }

      @Test
      void innerTest() {
         assertNotNull(collaborator);
         assertSame(collaborator, cut.getCollaborator());
      }

      @Test
      void innerTestWithMockParameter(@Injectable("123") int number) {
         assertEquals(123, number);
      }
   }
}

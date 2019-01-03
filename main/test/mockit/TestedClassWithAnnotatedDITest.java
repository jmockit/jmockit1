package mockit;

import javax.annotation.*;
import javax.inject.*;

import org.junit.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.springframework.beans.factory.annotation.*;

@FixMethodOrder(NAME_ASCENDING)
public final class TestedClassWithAnnotatedDITest
{
   static class TestedClass1 {
      @Resource(name = "secondAction") Runnable action2;
      @Autowired int someValue;
      @Resource(name = "firstAction") Runnable action1;
      @Resource(name = "thirdAction") Runnable action3;
      @Inject int anotherValue;
      @Value("textValue") String stringFieldWithValue;
      @Value("123.45") double numericFieldWithValue;
      @Value("#{systemProperties.someProperty}") String systemProperty;
      @Value("${anotherSystemProperty}") int anInt;
   }

   static class TestedClass2 {
      final int someValue;
      final Runnable action;
      @Resource Runnable anotherAction;
      String text;
      @Inject String anotherText;
      @Autowired(required = false) Runnable optionalAction;

      @Autowired
      TestedClass2(int someValue, Runnable action, String textValue) {
         this.someValue = someValue;
         this.action = action;
         text = textValue;
      }
   }

   @Tested TestedClass1 tested1;
   @Tested TestedClass2 tested2;
   @Injectable Runnable firstAction;
   @Injectable final int someValue = 1;
   @Injectable Runnable action;
   @Injectable String textValue = "test";
   @Injectable String anotherText = "name2";
   @Injectable Runnable action3; // matches @Resource(name = "thirdAction") by field name, after failing to match on "thirdAction"

   @Test
   public void injectAllAnnotatedInjectionPoints(
      @Injectable("2") int anotherValue, @Injectable Runnable secondAction, @Injectable Runnable anotherAction,
      @Injectable("true") boolean unused, @Injectable("test") String stringFieldWithValue,
      @Injectable("123.45") double numericFieldWithValue, @Injectable("propertyValue") String systemProperty, @Injectable("123") int anInt
   ) {
      assertSame(firstAction, tested1.action1);
      assertSame(secondAction, tested1.action2);
      assertSame(action3, tested1.action3);
      assertEquals(1, tested1.someValue);
      assertEquals(2, tested1.anotherValue);
      assertEquals("test", tested1.stringFieldWithValue);
      assertEquals(123.45, tested1.numericFieldWithValue, 0);
      assertEquals("propertyValue", tested1.systemProperty);
      assertEquals(123, tested1.anInt);

      assertEquals(1, tested2.someValue);
      assertSame(action, tested2.action);
      assertSame(anotherAction, tested2.anotherAction);
      assertSame(textValue, tested2.text);
      assertSame(anotherText, tested2.anotherText);
      assertNull(tested2.optionalAction);
   }

   @Test
   public void leaveValueAnnotatedInjectionPointsWithDefaultInitializationValue(
      @Injectable Runnable action2, @Injectable Runnable anotherAction, @Injectable("2") int anotherValue
   ) {
      assertNull(tested1.systemProperty);
      assertEquals(0, tested1.anInt);
   }

   @Test(expected = IllegalStateException.class)
   public void failForAnnotatedFieldWhichLacksAnInjectable() {
      fail("Must fail before starting");
   }

   @Test(expected = IllegalStateException.class)
   public void failForAnnotatedFieldHavingAnInjectableOfTheSameTypeWhichWasAlreadyConsumed(
      @Injectable Runnable secondAction
   ) {
      fail("Must fail before starting");
   }
}

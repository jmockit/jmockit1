/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.junit.*;
import org.junit.rules.*;
import static org.junit.Assert.*;

import mockit.internal.util.*;
import static mockit.Deencapsulation.*;

import static org.hamcrest.CoreMatchers.*;

@SuppressWarnings("unused")
public final class DeencapsulationTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   static final class Subclass extends BaseClass
   {
      final int INITIAL_VALUE = new Random().nextInt();
      final int initialValue = -1;

      private static final Integer constantField = 123;

      private static StringBuilder buffer;
      private static char static1;
      private static char static2;

      static StringBuilder getBuffer() { return buffer; }
      static void setBuffer(StringBuilder buffer) { Subclass.buffer = buffer; }

      private String stringField;
      private int intField;
      private int intField2;
      private List<String> listField;

      Subclass() { intField = -1; }
      Subclass(int a, String b) { intField = a; stringField = b; }
      Subclass(String... args) { listField = Arrays.asList(args); }
      Subclass(List<String> list) { listField = list; }

      private static Boolean anStaticMethod() { return true; }
      private static void staticMethod(short s, String str, Boolean b) {}
      private static String staticMethod(short s, StringBuilder str, boolean b) { return String.valueOf(str); }

      private long aMethod() { return 567L; }
      private void instanceMethod(short s, String str, Boolean b) {}
      private String instanceMethod(short s, StringBuilder str, boolean b) { return String.valueOf(str); }

      int getIntField() { return intField; }
      void setIntField(int intField) { this.intField = intField; }

      int getIntField2() { return intField2; }
      void setIntField2(int intField2) { this.intField2 = intField2; }

      String getStringField() { return stringField; }
      void setStringField(String stringField) { this.stringField = stringField; }

      List<String> getListField() { return listField; }
      void setListField(List<String> listField) { this.listField = listField; }

      private final class InnerClass
      {
         private InnerClass() {}
         private InnerClass(boolean b, Long l, String s) {}
         private InnerClass(List<String> list) {}
      }
   }

   static final Class<?> innerClass = ClassLoad.loadClass(Subclass.class.getName() + "$InnerClass");
   final Subclass anInstance = new Subclass();

   @Test
   public void getInstanceFieldByName()
   {
      anInstance.setIntField(3);
      anInstance.setStringField("test");
      anInstance.setListField(Collections.<String>emptyList());

      Integer intValue = getField(anInstance, "intField");
      String stringValue = getField(anInstance, "stringField");
      List<String> listValue = getField(anInstance, "listField");

      assertEquals(anInstance.getIntField(), intValue.intValue());
      assertEquals(anInstance.getStringField(), stringValue);
      assertSame(anInstance.getListField(), listValue);
   }

   @Test
   public void attemptToGetInstanceFieldByNameWithWrongName()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("No instance field of name \"noField\" found");

      getField(anInstance, "noField");
   }

   @Test
   public void getInheritedInstanceFieldByName()
   {
      anInstance.baseInt = 3;
      anInstance.baseString = "test";
      anInstance.baseSet = Collections.emptySet();

      Integer intValue = getField(anInstance, "baseInt");
      String stringValue = getField(anInstance, "baseString");
      Set<Boolean> listValue = getField(anInstance, "baseSet");

      assertEquals(anInstance.baseInt, intValue.intValue());
      assertEquals(anInstance.baseString, stringValue);
      assertSame(anInstance.baseSet, listValue);
   }

   @Test @SuppressWarnings("unchecked")
   public void getInstanceFieldByType()
   {
      anInstance.setStringField("by type");
      anInstance.setListField(new ArrayList<String>());

      String stringValue = getField(anInstance, String.class);
      List<String> listValue = getField(anInstance, List.class);
      List<String> listValue2 = getField(anInstance, ArrayList.class);

      assertEquals(anInstance.getStringField(), stringValue);
      assertSame(anInstance.getListField(), listValue);
      assertSame(listValue, listValue2);
   }

   @Test
   public void attemptToGetInstanceFieldByTypeWithWrongType()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Instance field of type byte or Byte not found");

      getField(anInstance, Byte.class);
   }

   @Test
   public void attemptToGetInstanceFieldByTypeForClassWithMultipleFieldsOfThatType()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("More than one instance field");
      thrown.expectMessage("of type int ");
      thrown.expectMessage("INITIAL_VALUE, initialValue");

      getField(anInstance, int.class);
   }

   @Test @SuppressWarnings("unchecked")
   public void getInheritedInstanceFieldByType()
   {
      Set<Boolean> fieldValueOnInstance = new HashSet<Boolean>();
      anInstance.baseSet = fieldValueOnInstance;

      Set<Boolean> setValue = getField(anInstance, fieldValueOnInstance.getClass());
      Set<Boolean> setValue2 = getField(anInstance, HashSet.class);

      assertSame(fieldValueOnInstance, setValue);
      assertSame(setValue, setValue2);
   }

   @Test
   public void getInstanceFieldOnBaseClassByType()
   {
      anInstance.setLongField(15);

      long longValue = getField(anInstance, long.class);

      assertEquals(15, longValue);
   }

   @Test
   public void getStaticFieldByName()
   {
      Subclass.setBuffer(new StringBuilder());

      StringBuilder b = getField(Subclass.class, "buffer");

      assertSame(Subclass.getBuffer(), b);
   }

   @Test
   public void attemptToGetStaticFieldByNameFromWrongClass()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("No static field of name \"buffer\" found in class mockit.BaseClass");

      getField(BaseClass.class, "buffer");
   }

   @Test
   public void getStaticFieldByType()
   {
      Subclass.setBuffer(new StringBuilder());

      StringBuilder b = getField(Subclass.class, StringBuilder.class);

      assertSame(Subclass.getBuffer(), b);
   }

   @Test
   public void setInstanceFieldByName()
   {
      anInstance.setIntField2(1);

      setField(anInstance, "intField2", 901);

      assertEquals(901, anInstance.getIntField2());
   }

   @Test
   public void attemptToSetInstanceFieldByNameWithWrongName()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("No instance field of name \"noField\" found");

      setField(anInstance, "noField", 901);
   }

   @Test
   public void setInstanceFieldByType()
   {
      anInstance.setStringField("");

      setField(anInstance, "Test");

      assertEquals("Test", anInstance.getStringField());
   }

   @Test
   public void attemptToSetInstanceFieldByTypeWithWrongType()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Instance field of type byte or Byte not found");

      setField(anInstance, (byte) 123);
   }

   @Test
   public void attemptToSetInstanceFieldByTypeForClassWithMultipleFieldsOfThatType()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("More than one instance field ");

      setField(anInstance, 901);
   }

   @Test
   public void setStaticFieldByName()
   {
      Subclass.setBuffer(null);

      setField(Subclass.class, "buffer", new StringBuilder());

      assertNotNull(Subclass.getBuffer());
   }

   @Test
   public void attemptToSetStaticFieldByNameWithWrongName()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("No static field of name \"noField\" found ");

      setField(Subclass.class, "noField", null);
   }

   @Test
   public void setStaticFieldByType()
   {
      Subclass.setBuffer(null);

      setField(Subclass.class, new StringBuilder());

      assertNotNull(Subclass.getBuffer());
   }

   @Test
   public void attemptToSetStaticFieldByTypeWithWrongType()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Static field of type StringBuffer not found");

      setField(Subclass.class, new StringBuffer());
   }

   @Test
   public void attemptToSetStaticFieldByTypeForClassWithMultipleFieldsOfThatType()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("More than one static field ");

      setField(Subclass.class, 'A');
   }

   @Test
   public void setFinalInstanceFields()
   {
      Subclass obj = new Subclass();

      setField(obj, "INITIAL_VALUE", 123);
      setField(obj, "initialValue", 123);

      assertEquals(123, obj.INITIAL_VALUE);
      assertEquals(123, getField(obj, "initialValue"));
      assertEquals(-1, obj.initialValue); // in this case, the compile-time constant gets embedded in client code
   }

   @Test
   public void attemptToSetAStaticFinalField()
   {
      thrown.expectCause(isA(IllegalAccessException.class));

      setField(Subclass.class, "constantField", 54);
   }

   @Test
   public void invokeInstanceMethodWithoutParameters()
   {
      Long result = invoke(anInstance, "aMethod");

      assertEquals(567L, result.longValue());
   }

   @Test
   public void invokeInstanceMethodWithSpecifiedParameterTypes()
   {
      String result =
         invoke(
            anInstance, "instanceMethod",
            new Class<?>[] {short.class, StringBuilder.class, boolean.class},
            (short) 7, new StringBuilder("abc"), true);

      assertEquals("abc", result);
   }

   @Test
   public void invokeInstanceMethodWithMultipleParameters()
   {
      assertNull(invoke(anInstance, "instanceMethod", (short) 7, "abc", true));

      String result = invoke(anInstance, "instanceMethod", (short) 7, new StringBuilder("abc"), true);
      assertEquals("abc", result);
   }

   @Test
   public void invokeInstanceMethodWithNullArgumentsSpecifiedThroughClassLiterals()
   {
      assertNull(invoke(anInstance, "instanceMethod", (short) 7, String.class, Boolean.class));

      String result = invoke(anInstance, "instanceMethod", (short) 7, StringBuilder.class, true);
      assertEquals("null", result);
   }

   @Test
   public void invokeInstanceMethodWithInvalidNullArgument()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Invalid null ");
      thrown.expectMessage(" argument 1");

      invoke(anInstance, "instanceMethod", (short) 7, null, true);
   }

   @Test
   public void invokeInstanceMethodWithNullVarargs()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Invalid null value passed as argument");

      //noinspection NullArgumentToVariableArgMethod
      invoke(anInstance, "instanceMethod", null);
   }

   @Test
   public void invokeInstanceMethodWithSingleParameterOfBaseTypeWhilePassingSubtypeInstance()
   {
      invoke(anInstance, "setListField", new ArrayList<String>());
   }

   @Test
   public void invokeStaticMethodWithoutParameters()
   {
      Boolean result = invoke(Subclass.class, "anStaticMethod");

      assertTrue(result);
   }

   @Test
   public void invokeStaticMethodByClassNameWithoutParameters()
   {
      Boolean result = invoke(Subclass.class.getName(), "anStaticMethod");

      assertTrue(result);
   }

   @Test
   public void invokeStaticMethodWithSpecifiedParameterTypes()
   {
      String result =
         invoke(
            Subclass.class, "staticMethod",
            new Class<?>[] {short.class, StringBuilder.class, boolean.class},
            (short) 7, new StringBuilder("abc"), true);

      assertEquals("abc", result);
   }

   @Test
   public void invokeStaticMethodWithMultipleParameters()
   {
      assertNull(invoke(Subclass.class, "staticMethod", (short) 7, "abc", true));

      String result = invoke(Subclass.class, "staticMethod", (short) 7, new StringBuilder("abc"), true);
      assertEquals("abc", result);
   }

   @Test
   public void invokeStaticMethodByClassNameWithMultipleParameters()
   {
      Object result = invoke(Subclass.class.getName(), "staticMethod", (short) 7, "abc", true);

      assertNull(result);
   }

   @Test
   public void invokeMethodByClassNameOnUnavailableClass()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("No class with name \"abc.xyz.NoClass\" found");

      invoke("abc.xyz.NoClass", "aMethod");
   }

   @Test
   public void invokeStaticMethodWithNullArgumentsSpecifiedThroughClassLiterals()
   {
      assertNull(invoke(anInstance, "staticMethod", (short) 7, String.class, Boolean.class));

      String result = invoke(anInstance, "staticMethod", (short) 7, StringBuilder.class, true);
      assertEquals("null", result);
   }

   @Test
   public void invokeStaticMethodWithInvalidNullArgument()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Invalid null value ");
      thrown.expectMessage(" argument 1");

      invoke(anInstance, "staticMethod", (short) 7, null, true);
   }

   @Test
   public void invokeMethodWithNonMatchingArrayArguments()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("No compatible method found");
      thrown.expectMessage("int[]");
      thrown.expectMessage("String[]");
      thrown.expectMessage("java.util.List[]");

      invoke(anInstance, "aMethod", "test", 1, 2.0, new int[0], new String[0], new List<?>[0], new ArrayList<Long>());
   }

   @Test
   public void invokeInstanceMethodAsAnStaticMethod()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Attempted to invoke non-static method without an instance");

      invoke(anInstance.getClass(), "instanceMethod", (short) 7, "test", true);
   }

   @Test
   public void invokeInstanceMethodAsAnStaticMethodUsingClassName()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Attempted to invoke non-static method without an instance");

      invoke(anInstance.getClass().getName(), "instanceMethod", (short) 7, "test", true);
   }

   @Test
   public void newInstanceUsingNoArgsConstructorFromSpecifiedParameterTypes()
   {
      Class<?>[] parameterTypes = {};
      Subclass instance = newInstance(Subclass.class.getName(), (Object[]) parameterTypes);

      assertNotNull(instance);
      assertEquals(-1, instance.getIntField());
   }

   @Test
   public void newInstanceUsingNoArgsConstructorWithoutSpecifyingParameters()
   {
      Subclass instance = newInstance(Subclass.class.getName());

      assertNotNull(instance);
      assertEquals(-1, instance.getIntField());
   }

   @Test
   public void invokeConstructorWithNullVarargs()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Invalid null value passed as argument");

      //noinspection NullArgumentToVariableArgMethod
      newInstance(Subclass.class.getName(), null);
   }

   @Test
   public void newInstanceByNameUsingMultipleArgsConstructorFromSpecifiedParameterTypes()
   {
      Subclass instance = newInstance(Subclass.class.getName(), new Class<?>[] {int.class, String.class}, 1, "XYZ");

      assertNotNull(instance);
      assertEquals(1, instance.getIntField());
      assertEquals("XYZ", instance.getStringField());
   }

   @Test
   public void newInstanceUsingMultipleArgsConstructorFromSpecifiedParameterTypes()
   {
      BaseClass instance = newInstance(Subclass.class, new Class<?>[] {int.class, String.class}, 1, "XYZ");

      assertNotNull(instance);
   }

   @Test
   public void attemptNewInstanceWithNoMatchingConstructor()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Specified constructor not found: Subclass(char)");

      newInstance(Subclass.class.getName(), new Class<?>[] {char.class}, 'z');
   }

   @Test
   public void newInstanceByNameUsingMultipleArgsConstructorFromNonNullArgumentValues()
   {
      Subclass instance = newInstance(Subclass.class.getName(), 590, "");

      assertNotNull(instance);
      assertEquals(590, instance.getIntField());
      assertEquals("", instance.getStringField());
   }

   @Test
   public void newInstanceByNameUsingMultipleArgsConstructorWithInvalidNullArgument()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Invalid null value");
      thrown.expectMessage(" argument 1");

      newInstance(Subclass.class.getName(), 590, null);
   }

   @Test
   public void newInstancePassingSubclassInstanceToConstructorWithSingleArgument()
   {
      List<String> aList = new ArrayList<String>();

      Subclass instance = newInstance(Subclass.class, aList);

      assertNotNull(instance);
      assertSame(aList, instance.getListField());
   }

   @Test
   public void newInstanceUsingMultipleArgsConstructorFromNonNullArgumentValues()
   {
      BaseClass instance = newInstance(Subclass.class, 590, "");

      assertNotNull(instance);
   }

   @Test
   public void newInnerInstanceUsingNoArgsConstructor()
   {
      Object innerInstance = newInnerInstance("InnerClass", anInstance);

      assertTrue(innerClass.isInstance(innerInstance));
   }

   class InnerClass { InnerClass(int i) {} }

   @Test
   public void instantiateInnerClassWithOwnerInstance()
   {
      InnerClass ic = newInstance(InnerClass.class, this, 123);
      assertNotNull(ic);
   }

   @Test
   public void attemptToInstantiateInnerClassWithoutOwnerInstance()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("instantiation of inner class");

      newInstance(InnerClass.class, 123);
   }

   @Test
   public void newInnerInstanceWithWrongInnerClassName()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("No class with name ");
      thrown.expectMessage("$NoClass\" found");

      newInnerInstance("NoClass", anInstance);
   }

   @Test
   public void invokeConstructorForInnerClassWithNullVarargs()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Invalid null value passed as argument");

      //noinspection NullArgumentToVariableArgMethod
      newInnerInstance("InnerClass", anInstance, null);
   }

   @Test
   public void newInnerInstanceByNameUsingMultipleArgsConstructor()
   {
      Object innerInstance = newInnerInstance("InnerClass", anInstance, true, 5L, "");

      assertTrue(innerClass.isInstance(innerInstance));
   }

   @Test
   public void newInnerInstanceByNameUsingMultipleArgsConstructorWithInvalidNullArguments()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Invalid null value");

      newInnerInstance("InnerClass", anInstance, false, null, null);
   }

   @Test
   public void newInnerInstanceUsingMultipleArgsConstructor()
   {
      Object innerInstance = newInnerInstance(innerClass, anInstance, true, 5L, "");

      assertTrue(innerClass.isInstance(innerInstance));
   }

   @Test
   public void newInnerInstancePassingSubclassInstanceToConstructorWithSingleArgument()
   {
      List<String> aList = new ArrayList<String>();

      Object innerInstance = newInnerInstance(innerClass, anInstance, aList);

      assertTrue(innerClass.isInstance(innerInstance));
   }

   @Test
   public void attemptToInstantiateNestedClassAsIfItWasInnerClass()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Subclass is not an inner class");

      newInnerInstance(Subclass.class, this);
   }

   @Test(expected = InstantiationException.class)
   public void causeInstantiationExceptionToBeThrown() throws Exception
   {
      //noinspection ClassNewInstance
      Runnable.class.newInstance();
   }

   @Test
   public void newUninitializedInstanceOfConcreteClass()
   {
      Subclass instance = newUninitializedInstance(Subclass.class);

      assertEquals(0, instance.intField);
      assertEquals(0, instance.INITIAL_VALUE);
      assertEquals(-1, instance.initialValue);

      // This field value is a compile-time constant, so we need Reflection to read its current value:
      int initialValue = getField(instance, "initialValue");
      assertEquals(0, initialValue);
   }

   public abstract static class AbstractClass implements Runnable { protected abstract int doSomething(); }

   @Test
   public void newUninitializedInstanceOfAbstractClass()
   {
      AbstractClass instance = newUninitializedInstance(AbstractClass.class);

      assertNotNull(instance);
      assertEquals(0, instance.doSomething());
      instance.run();
   }

   @Test
   public void newUninitializedInstanceOfAbstractJREClass() throws Exception
   {
      Writer instance = newUninitializedInstance(Writer.class);

      assertNotNull(instance);
      assertNull(getField(instance, "lock"));

      // Abstract methods.
      instance.write(new char[0], 0, 0);
      instance.flush();
      instance.close();

      // Regular methods.
      try {
         instance.write(123);
         fail();
      }
      catch (NullPointerException ignore) {}
   }

   @Test
   public void newUninitializedInstanceOfInterface() throws Exception
   {
      Callable<?> callable = newUninitializedInstance(Callable.class);

      assertNotNull(callable);
      assertNull(callable.call());
   }

   @Test
   public void attemptToGetFieldOnMockedInstance(@Mocked BaseClass mock)
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("mocked instance");

      long l = getField(mock, "longField");
   }

   @Test
   public void attemptToSetFieldOnMockedInstance(@Injectable BaseClass mock)
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("mocked instance");

      setField(mock, "longField", 123L);
   }
}

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

import static org.hamcrest.CoreMatchers.*;

public final class DeencapsulationTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   static final class Subclass extends BaseClass
   {
      final int INITIAL_VALUE = new Random().nextInt();
      final int initialValue = -1;

      @SuppressWarnings("unused") private static final Integer constantField = 123;

      private static StringBuilder buffer;
      @SuppressWarnings("unused") private static char static1;
      @SuppressWarnings("unused") private static char static2;

      static StringBuilder getBuffer() { return buffer; }
      static void setBuffer(StringBuilder buffer) { Subclass.buffer = buffer; }

      private String stringField;
      private int intField;
      private int intField2;
      private List<String> listField;

      int getIntField() { return intField; }
      void setIntField(int intField) { this.intField = intField; }

      int getIntField2() { return intField2; }
      void setIntField2(int intField2) { this.intField2 = intField2; }

      String getStringField() { return stringField; }
      void setStringField(String stringField) { this.stringField = stringField; }

      List<String> getListField() { return listField; }
      void setListField(List<String> listField) { this.listField = listField; }
   }

   final Subclass anInstance = new Subclass();

   @Test
   public void getInstanceFieldByName()
   {
      anInstance.setIntField(3);
      anInstance.setStringField("test");
      anInstance.setListField(Collections.<String>emptyList());

      Integer intValue = Deencapsulation.getField(anInstance, "intField");
      String stringValue = Deencapsulation.getField(anInstance, "stringField");
      List<String> listValue = Deencapsulation.getField(anInstance, "listField");

      assertEquals(anInstance.getIntField(), intValue.intValue());
      assertEquals(anInstance.getStringField(), stringValue);
      assertSame(anInstance.getListField(), listValue);
   }

   @Test
   public void attemptToGetInstanceFieldByNameWithWrongName()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("No instance field of name \"noField\" found");

      Deencapsulation.getField(anInstance, "noField");
   }

   @Test
   public void getInheritedInstanceFieldByName()
   {
      anInstance.baseInt = 3;
      anInstance.baseString = "test";
      anInstance.baseSet = Collections.emptySet();

      Integer intValue = Deencapsulation.getField(anInstance, "baseInt");
      String stringValue = Deencapsulation.getField(anInstance, "baseString");
      Set<Boolean> listValue = Deencapsulation.getField(anInstance, "baseSet");

      assertEquals(anInstance.baseInt, intValue.intValue());
      assertEquals(anInstance.baseString, stringValue);
      assertSame(anInstance.baseSet, listValue);
   }

   @Test @SuppressWarnings("unchecked")
   public void getInstanceFieldByType()
   {
      anInstance.setStringField("by type");
      anInstance.setListField(new ArrayList<String>());

      String stringValue = Deencapsulation.getField(anInstance, String.class);
      List<String> listValue = Deencapsulation.getField(anInstance, List.class);
      List<String> listValue2 = Deencapsulation.getField(anInstance, ArrayList.class);

      assertEquals(anInstance.getStringField(), stringValue);
      assertSame(anInstance.getListField(), listValue);
      assertSame(listValue, listValue2);
   }

   @Test
   public void attemptToGetInstanceFieldByTypeWithWrongType()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Instance field of type byte or Byte not found");

      Deencapsulation.getField(anInstance, Byte.class);
   }

   @Test
   public void attemptToGetInstanceFieldByTypeForClassWithMultipleFieldsOfThatType()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("More than one instance field");
      thrown.expectMessage("of type int ");
      thrown.expectMessage("INITIAL_VALUE, initialValue");

      Deencapsulation.getField(anInstance, int.class);
   }

   @Test @SuppressWarnings("unchecked")
   public void getInheritedInstanceFieldByType()
   {
      Set<Boolean> fieldValueOnInstance = new HashSet<Boolean>();
      anInstance.baseSet = fieldValueOnInstance;

      Set<Boolean> setValue = Deencapsulation.getField(anInstance, fieldValueOnInstance.getClass());
      Set<Boolean> setValue2 = Deencapsulation.getField(anInstance, HashSet.class);

      assertSame(fieldValueOnInstance, setValue);
      assertSame(setValue, setValue2);
   }

   @Test
   public void getInstanceFieldOnBaseClassByType()
   {
      anInstance.setLongField(15);

      long longValue = Deencapsulation.getField(anInstance, long.class);

      assertEquals(15, longValue);
   }

   @Test
   public void getStaticFieldByName()
   {
      Subclass.setBuffer(new StringBuilder());

      StringBuilder b = Deencapsulation.getField(Subclass.class, "buffer");

      assertSame(Subclass.getBuffer(), b);
   }

   @Test
   public void attemptToGetStaticFieldByNameFromWrongClass()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("No static field of name \"buffer\" found in class mockit.BaseClass");

      Deencapsulation.getField(BaseClass.class, "buffer");
   }

   @Test
   public void getStaticFieldByType()
   {
      Subclass.setBuffer(new StringBuilder());

      StringBuilder b = Deencapsulation.getField(Subclass.class, StringBuilder.class);

      assertSame(Subclass.getBuffer(), b);
   }

   @Test
   public void setInstanceFieldByName()
   {
      anInstance.setIntField2(1);

      Deencapsulation.setField(anInstance, "intField2", 901);

      assertEquals(901, anInstance.getIntField2());
   }

   @Test
   public void attemptToSetInstanceFieldByNameWithWrongName()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("No instance field of name \"noField\" found");

      Deencapsulation.setField(anInstance, "noField", 901);
   }

   @Test
   public void setInstanceFieldByType()
   {
      anInstance.setStringField("");

      Deencapsulation.setField(anInstance, "Test");

      assertEquals("Test", anInstance.getStringField());
   }

   @Test
   public void attemptToSetInstanceFieldByTypeWithWrongType()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Instance field of type byte or Byte not found");

      Deencapsulation.setField(anInstance, (byte) 123);
   }

   @Test
   public void attemptToSetInstanceFieldByTypeForClassWithMultipleFieldsOfThatType()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("More than one instance field ");

      Deencapsulation.setField(anInstance, 901);
   }

   @Test
   public void setStaticFieldByName()
   {
      Subclass.setBuffer(null);

      Deencapsulation.setField(Subclass.class, "buffer", new StringBuilder());

      assertNotNull(Subclass.getBuffer());
   }

   @Test
   public void attemptToSetStaticFieldByNameWithWrongName()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("No static field of name \"noField\" found ");

      Deencapsulation.setField(Subclass.class, "noField", null);
   }

   @Test
   public void setStaticFieldByType()
   {
      Subclass.setBuffer(null);

      Deencapsulation.setField(Subclass.class, new StringBuilder());

      assertNotNull(Subclass.getBuffer());
   }

   @Test
   public void attemptToSetFieldByTypeWithoutAValue()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Missing field value");

      Deencapsulation.setField(Subclass.class, null);
   }

   @Test
   public void attemptToSetStaticFieldByTypeWithWrongType()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Static field of type StringBuffer not found");

      Deencapsulation.setField(Subclass.class, new StringBuffer());
   }

   @Test
   public void attemptToSetStaticFieldByTypeForClassWithMultipleFieldsOfThatType()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("More than one static field ");

      Deencapsulation.setField(Subclass.class, 'A');
   }

   @Test
   public void setFinalInstanceFields()
   {
      Subclass obj = new Subclass();

      Deencapsulation.setField(obj, "INITIAL_VALUE", 123);
      Deencapsulation.setField(obj, "initialValue", 123);

      assertEquals(123, obj.INITIAL_VALUE);
      assertEquals(123, Deencapsulation.getField(obj, "initialValue"));
      assertEquals(-1, obj.initialValue); // in this case, the compile-time constant gets embedded in client code
   }

   @Test
   public void attemptToSetAStaticFinalField()
   {
      thrown.expectCause(isA(IllegalAccessException.class));

      Deencapsulation.setField(Subclass.class, "constantField", 54);
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
      Subclass instance = Deencapsulation.newUninitializedInstance(Subclass.class);

      assertEquals(0, instance.intField);
      assertEquals(0, instance.INITIAL_VALUE);
      assertEquals(-1, instance.initialValue);

      // This field value is a compile-time constant, so we need Reflection to read its current value:
      int initialValue = Deencapsulation.getField(instance, "initialValue");
      assertEquals(0, initialValue);
   }

   public abstract static class AbstractClass implements Runnable { protected abstract int doSomething(); }

   @Test
   public void newUninitializedInstanceOfAbstractClass()
   {
      AbstractClass instance = Deencapsulation.newUninitializedInstance(AbstractClass.class);

      assertNotNull(instance);
      assertEquals(0, instance.doSomething());
      instance.run();
   }

   @Test
   public void newUninitializedInstanceOfAbstractJREClass() throws Exception
   {
      Writer instance = Deencapsulation.newUninitializedInstance(Writer.class);

      assertNotNull(instance);
      assertNull(Deencapsulation.getField(instance, "lock"));

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
      Callable<?> callable = Deencapsulation.newUninitializedInstance(Callable.class);

      assertNotNull(callable);
      assertNull(callable.call());
   }
}

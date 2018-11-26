package otherTests;

import java.util.*;

import org.junit.*;
import org.junit.rules.*;
import static org.junit.Assert.*;

import mockit.*;

public final class DeencapsulationTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   static final class Subclass extends BaseClass
   {
      @SuppressWarnings("unused") final int initialValue = -1;

      @SuppressWarnings("unused") private static final Integer constantField = 123;

      private static StringBuilder buffer;
      @SuppressWarnings("unused") private static char static1;
      @SuppressWarnings("unused") private static char static2;

      static StringBuilder getBuffer() { return buffer; }
      static void setBuffer(StringBuilder buffer) { Subclass.buffer = buffer; }

      private String stringField;
      private int intField;
      private List<String> listField;

      int getIntField() { return intField; }
      void setIntField(int intField) { this.intField = intField; }

      String getStringField() { return stringField; }
      void setStringField(String stringField) { this.stringField = stringField; }

      List<String> getListField() { return listField; }
      void setListField(List<String> listField) { this.listField = listField; }
   }

   final Subclass anInstance = new Subclass();

   @Test
   public void getInstanceFieldByName() {
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
   public void attemptToGetInstanceFieldByNameWithWrongName() {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("No instance field of name \"noField\" found");

      Deencapsulation.getField(anInstance, "noField");
   }

   @Test
   public void getInheritedInstanceFieldByName() {
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
   public void getInstanceFieldByType() {
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
   public void attemptToGetInstanceFieldByTypeWithWrongType() {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Instance field of type byte or Byte not found");

      Deencapsulation.getField(anInstance, Byte.class);
   }

   @Test
   public void attemptToGetInstanceFieldByTypeForClassWithMultipleFieldsOfThatType() {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("More than one instance field");
      thrown.expectMessage("of type int ");
      thrown.expectMessage("initialValue");

      Deencapsulation.getField(anInstance, int.class);
   }

   @Test @SuppressWarnings("unchecked")
   public void getInheritedInstanceFieldByType() {
      Set<Boolean> fieldValueOnInstance = new HashSet<>();
      anInstance.baseSet = fieldValueOnInstance;

      Set<Boolean> setValue = Deencapsulation.getField(anInstance, fieldValueOnInstance.getClass());
      Set<Boolean> setValue2 = Deencapsulation.getField(anInstance, HashSet.class);

      assertSame(fieldValueOnInstance, setValue);
      assertSame(setValue, setValue2);
   }

   @Test
   public void getInstanceFieldOnBaseClassByType() {
      anInstance.setLongField(15);

      long longValue = Deencapsulation.getField(anInstance, long.class);

      assertEquals(15, longValue);
   }

   @Test
   public void getStaticFieldByName() {
      Subclass.setBuffer(new StringBuilder());

      StringBuilder b = Deencapsulation.getField(Subclass.class, "buffer");

      assertSame(Subclass.getBuffer(), b);
   }

   @Test
   public void attemptToGetStaticFieldByNameFromWrongClass() {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("No static field of name \"buffer\" found in class otherTests.BaseClass");

      Deencapsulation.getField(BaseClass.class, "buffer");
   }

   @Test
   public void getStaticFieldByType() {
      Subclass.setBuffer(new StringBuilder());

      StringBuilder b = Deencapsulation.getField(Subclass.class, StringBuilder.class);

      assertSame(Subclass.getBuffer(), b);
   }
}

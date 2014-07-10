package integrationTests.data;

public class ClassWithFieldsAccessedByMultipleTests
{
   private static boolean staticField1;
   private static boolean staticField2;

   private long instanceField1;
   private long instanceField2;

   public static boolean isStaticField1()
   {
      return staticField1;
   }

   public static void setStaticField1(boolean staticField)
   {
      staticField1 = staticField;
   }

   public static boolean isStaticField2()
   {
      return staticField2;
   }

   public static void setStaticField2(boolean staticField)
   {
      staticField2 = staticField;
   }

   public long getInstanceField1()
   {
      return instanceField1;
   }

   public void setInstanceField1(long instanceField)
   {
      instanceField1 = instanceField;
   }

   public long getInstanceField2()
   {
      return instanceField2;
   }

   public void setInstanceField2(long instanceField)
   {
      instanceField2 = instanceField;
   }
}
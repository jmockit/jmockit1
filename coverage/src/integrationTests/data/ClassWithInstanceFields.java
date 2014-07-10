/*
   File header followed by blank line.
 */

package integrationTests.data;

@SuppressWarnings({"ClassWithTooManyFields"})
public final class ClassWithInstanceFields
{
   private final int finalField;

   private boolean booleanField;
   private byte byteField;
   private char charField;
   public short shortField;
   private int intField;
   private long longField;
   private float floatField;
   private double doubleField;

   private int[] arrayField;

   public ClassWithInstanceFields()
   {
      finalField = 123;
   }

   public int getFinalField()
   {
      return finalField;
   }

   public boolean isBooleanField()
   {
      return booleanField;
   }

   public void setBooleanField(boolean booleanField)
   {
      this.booleanField = booleanField;
   }

   public byte getByteField()
   {
      return byteField;
   }

   public void setByteField(byte byteField)
   {
      this.byteField = byteField;
   }

   public char getCharField()
   {
      return charField;
   }

   public void setCharField(char charField)
   {
      this.charField = charField;
   }

   public void setShortField(short shortField)
   {
      this.shortField = shortField;
   }

   public int getIntField()
   {
      return intField;
   }

   public void setIntField(int intField)
   {
      this.intField = intField;
   }

   public long getLongField()
   {
      return longField;
   }

   public void setLongField(long longField)
   {
      this.longField = longField;
   }

   public float getFloatField()
   {
      return floatField;
   }

   public void setFloatField(float floatField)
   {
      this.floatField = floatField;
   }

   public double getDoubleField()
   {
      return doubleField;
   }

   public void setDoubleField(double doubleField)
   {
      this.doubleField = doubleField;
   }

   public int[] getArrayField()
   {
      return arrayField;
   }

   public void setArrayField(int[] arrayField)
   {
      this.arrayField = arrayField;
   }
}
package otherTests;

import java.util.*;

public class BaseClass
{
   protected int baseInt;
   protected String baseString;
   protected Set<Boolean> baseSet;

   @SuppressWarnings({"FieldCanBeLocal", "unused"})
   private long longField;

   void setLongField(long value) { longField = value; }
}

package integrationTests;

import java.lang.annotation.*;

import static java.lang.annotation.RetentionPolicy.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

public final class MiscellaneousTest
{
   @Test
   public void methodWithIINCWideInstruction()
   {
      int i = 0;
      i += 1000; // compiled to opcode iinc_w
      assert i == 1000;
   }

   @Retention(RUNTIME) public @interface Dummy { Class<?> value(); }
   @Dummy(String.class) static class AnnotatedClass {}

   @Test
   public void havingAnnotationWithClassValue(@Injectable AnnotatedClass dummy)
   {
      assertNotNull(dummy);
   }
}

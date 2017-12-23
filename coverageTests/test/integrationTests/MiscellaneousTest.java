package integrationTests;

import java.beans.*;
import java.lang.annotation.*;
import java.lang.reflect.*;

import javax.annotation.*;
import static java.lang.annotation.RetentionPolicy.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

@Resources({@Resource(lookup = "Test", shareable = false), @Resource(type = int.class)})
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

   @Test
   public void verifyAnnotationsArePreserved() throws Exception
   {
      Constructor<ClassWithAnnotations> constructor = ClassWithAnnotations.class.getDeclaredConstructor();

      assertTrue(constructor.isAnnotationPresent(ConstructorProperties.class));
   }

   @Test
   public void mockingAnAnnotation(@Tested @Mocked AnAnnotation mockedAnnotation)
   {
      assertNull(mockedAnnotation.value());
   }
}

package java8testing;

import java.util.*;

import static java.util.Arrays.asList;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

import java8testing.LambdaExamples.*;

public final class LambdaExamplesTest
{
   @Tested LambdaExamples lambdaExamples;
   final List<Integer> ids = asList(0, -1);

   public static final class Abc extends Entity {}

   @Test
   public void noLambdas()
   {
      List<Abc> entities = lambdaExamples.noLambdas(Abc.class, ids);
      assertEquals(0, entities.size());
   }

   @Test
   public void withLambdas()
   {
      List<Abc> entities = lambdaExamples.withLambdas(Abc.class, ids);
      assertEquals(0, entities.size());
   }
}
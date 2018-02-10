package integrationTests;

import org.junit.*;

public final class ShutdownTest
{
   @Test
   public void addShutdownHookToExerciseSUTAfterTestRunHasFinished() {
      Runtime.getRuntime().addShutdownHook(new Thread() {
         @Override
         public void run() { exerciseSUT(); }
      });
   }

   void exerciseSUT() {
      new ClassNotExercised().doSomething(123, "not to be counted");
      ClassWithNestedClasses.doSomething();
   }
}

package integrationTests;

import java.util.concurrent.locks.*;

public final class ClassWithReferenceToNestedClass
{
   ClassWithReferenceToNestedClass() { new ReentrantReadWriteLock().readLock(); }

   public static boolean doSomething()
   {
      return true;
   }
}
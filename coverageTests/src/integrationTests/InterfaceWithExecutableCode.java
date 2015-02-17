package integrationTests;

import java.util.*;

public interface InterfaceWithExecutableCode
{
   int N = 1 + new Random().nextInt(10);

   void doSomething();
}
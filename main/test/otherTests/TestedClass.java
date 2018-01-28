package otherTests;

import mockit.integration.*;

public final class TestedClass
{
   private final MockedClass dependency;

   public TestedClass(MockedClass dependency) { this.dependency = dependency; }
   public boolean doSomething(int i) { return dependency.doSomething(i); }
}

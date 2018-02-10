package mockit.integration;

public final class MockedClass
{
   public String getValue() { return "REAL"; }
   public boolean doSomething(int i) { return i > 0; }
   public boolean doSomethingElse(int i) { return i < 0; }
}

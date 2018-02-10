package mockit.integration.junit4;

public final class AnotherDependency
{
   static boolean mockedAtSuiteLevel;
   public static boolean alwaysTrue() { return true; }
}

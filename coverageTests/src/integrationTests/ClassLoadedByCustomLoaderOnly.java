package integrationTests;

public final class ClassLoadedByCustomLoaderOnly
{
   private final String value;

   public ClassLoadedByCustomLoaderOnly(String value) { this.value = value; }

   public String getValue() { return value; }
}

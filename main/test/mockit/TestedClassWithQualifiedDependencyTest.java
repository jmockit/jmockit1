package mockit;

import java.lang.annotation.*;
import javax.inject.*;
import javax.xml.bind.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class TestedClassWithQualifiedDependencyTest
{
   public static class TestedClass
   {
      @Inject private Dependency1 dep1;
      @Autowired private Dependency2 dep2;
   }

   public static class Dependency1
   {
      @Named("action1") private Runnable action;
      @Qualifier("foo") private JAXBContext jaxb;
   }

   public static class Dependency2
   {
      @Qualifier("action2") private Runnable action;
      @Named("bar") private JAXBContext jaxb;
   }

   static final class Empty {}

   @Tested JAXBContext foo;

   @Before
   public void createJAXBContexts() throws Exception
   {
      foo = JAXBContext.newInstance(Empty.class);
   }

   @Tested Dependency2 dependency2;
   @Tested(fullyInitialized = true) TestedClass tested;
   @Injectable Runnable action1;

   @Test
   public void useTestedObjectWithDifferentDependenciesEachHavingAQualifiedSubDependency(@Injectable Runnable action2)
   {
      assertSame(action2, dependency2.action);
      assertSame(dependency2, tested.dep2);
      assertSame(action1, tested.dep1.action);
      assertSame(foo, tested.dep1.jaxb);
      assertNull(tested.dep2.jaxb);
   }
}

@Retention(RetentionPolicy.RUNTIME)
@interface Qualifier { String value() default ""; }

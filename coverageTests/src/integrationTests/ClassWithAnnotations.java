package integrationTests;

import java.beans.*;
import javax.sql.*;

@Deprecated
final class ClassWithAnnotations
{
   @SuppressWarnings("DefaultAnnotationParam")
   @AnAnnotation(integers = {})
   DataSource dataSource;

   @Deprecated
   @AnAnnotation(integers = {1, 2, 3})
   int[] values;

   @ConstructorProperties({"Ab", "cde"})
   ClassWithAnnotations() {}

   @AnAnnotation("some text") @Deprecated
   void aMethod() {}
}

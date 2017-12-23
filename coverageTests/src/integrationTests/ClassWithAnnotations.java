package integrationTests;

import java.beans.*;
import javax.annotation.*;
import javax.annotation.Resource.*;
import javax.sql.*;

@Resources(@Resource(name = "test"))
final class ClassWithAnnotations
{
   @SuppressWarnings("DefaultAnnotationParam")
   @AnAnnotation(integers = {})
   @Resource(authenticationType = AuthenticationType.APPLICATION)
   DataSource dataSource;

   @AnAnnotation(integers = {1, 2, 3})
   int[] values;

   @ConstructorProperties({"Ab", "cde"})
   ClassWithAnnotations() {}

   @AnAnnotation("some text")
   void aMethod() {}
}

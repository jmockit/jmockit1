/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package powermock.examples.annotationbased;

import org.junit.*;

import mockit.*;

import static org.junit.Assert.*;
import powermock.examples.annotationbased.dao.*;

/**
 * <a href="http://code.google.com/p/powermock/source/browse/trunk/examples/DocumentationExamples/src/test/java/powermock/examples/annotationbased/UsingMockAnnotationTest.java">PowerMock version</a>
 */
public final class UsingMockAnnotation_JMockit_Test
{
   @Injectable SomeDao someDaoMock;
   @Tested SomeService someService;

   @Test
   public void getData()
   {
      final Object data = new Object();

      new Expectations() {{ someDaoMock.getSomeData(); result = data; }};

      assertSame(data, someService.getData());
   }
}

/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package powermock.examples.annotationbased;

import org.junit.*;

import mockit.*;

import static org.junit.Assert.*;
import powermock.examples.annotationbased.dao.*;

/**
 * In JMockit, there is also <em>dynamic partial mocking</em>, as demonstrated in
 * {@link powermock.examples.annotationbased.DynamicPartialMock_JMockit_Test}.
 *
 * <a href="http://code.google.com/p/powermock/source/browse/trunk/examples/DocumentationExamples/src/test/java/powermock/examples/annotationbased/UsingMockNiceAnnotationToCreateAPartialMockTest.java">PowerMock version</a>
 */
public final class UsingMockNiceAnnotationToCreateAPartialMock_JMockit_Test
{
   @Injectable @Mocked("getSomeData") SomeDao someDaoMock;
   @Tested SomeService someService;

   @Test
   public void assertThatNiceMockAnnotationWork()
   {
      assertNull(someService.getData());
      assertNotNull(someService.getMoreData());
   }
}

/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.easymock.samples;

import org.junit.*;

import mockit.*;

public final class BasicClassMock_JMockit_Test
{
   @Tested Document document;
   @Injectable Printer printer;

   @Test
   public void testPrintContent()
   {
      document.setContent("Hello world");
      document.print();

      new Verifications() {{ printer.print("Hello world"); }};
   }

   @Test
   public void testPrintEmptyContent()
   {
      document.setContent("");
      document.print();

      new Verifications() {{ printer.print(""); }};
   }
}

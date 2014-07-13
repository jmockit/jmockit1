/*
 * Copyright 2003-2009 OFFIS, Henri Tremblay
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.easymock.samples;

import org.junit.*;
import org.junit.runner.*;

import org.easymock.*;

public final class BasicClassMockTest extends EasyMockSupport
{
   // Neither @Mock nor @TestSubject can be used here.
   // The first, because the field is still null in the @Before method;
   // the second, because it does not instantiate the tested class at all.
   private Printer printer;
   private Document document;

   @Before
   public void setUp()
   {
      printer = createMock(Printer.class);
      document = new Document(printer);
   }

   @Test
   public void testPrintContent()
   {
      printer.print("Hello world");

      replayAll();

      document.setContent("Hello world");
      document.print();

      verifyAll(); // make sure Printer.print was called
   }

   @Test
   public void testPrintEmptyContent()
   {
      printer.print("");
      replayAll();

      document.setContent("");
      document.print();

      verifyAll(); // make sure Printer.print was called
   }
}

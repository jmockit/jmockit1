/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import mockit.internal.startup.*;
import org.junit.*;
import org.junit.rules.*;
import static mockit.Deencapsulation.*;
import static mockit.internal.startup.Startup.*;

public final class VersionVerificationTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   @Test
   public void checkVersionOnInitializeIfPossible()
   {
      thrown.expect(RuntimeException.class);
      thrown.expectMessage("JMockit with version '<already loaded version>' is already loaded in this JVM, JMockit with version '" + getField(Startup.class, "VERSION") + "' could not be loaded!");

      String originalVersion = getField(Startup.class, "version");
      try {
         setField(Startup.class, "version", "<already loaded version>");
         initializeIfPossible();
      } finally {
         setField(Startup.class, "version", originalVersion);
      }
   }

   @Test
   public void checkVersionOnVerifyInitialization()
   {
      thrown.expect(RuntimeException.class);
      thrown.expectMessage("JMockit with version '<already loaded version>' is already loaded in this JVM, JMockit with version '" + getField(Startup.class, "VERSION") + "' could not be loaded!");

      String originalVersion = getField(Startup.class, "version");
      try {
         setField(Startup.class, "version", "<already loaded version>");
         verifyInitialization();
      } finally {
         setField(Startup.class, "version", originalVersion);
      }
   }
}

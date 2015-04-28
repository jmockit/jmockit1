/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package java8testing;

import java.time.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

public final class TimeAPIMockingTest
{
   @Test
   public void mockClock()
   {
      // Create a test clock with a fixed instant.
      LocalDateTime testDateTime = LocalDateTime.parse("2014-05-10T10:15:30");
      ZoneId zoneId = ZoneId.systemDefault();
      Instant testInstant = testDateTime.toInstant(zoneId.getRules().getOffset(testDateTime));
      Clock testClock = Clock.fixed(testInstant, zoneId);

      // In production code, obtain local date & time from the test clock.
      LocalDateTime now = LocalDateTime.now(testClock);

      assertEquals(testDateTime, now);
   }

   @Test
   public void mockLocalDateTime()
   {
      LocalDateTime testDateTime = LocalDateTime.parse("2014-05-10T09:35:12");

      new Expectations(LocalDateTime.class) {{ LocalDateTime.now(); result = testDateTime; }};

      LocalDateTime now = LocalDateTime.now();

      assertSame(testDateTime, now);
   }

   @Test
   public void mockInstant()
   {
      Instant testInstant = Instant.parse("2014-05-10T09:35:12Z");

      new Expectations(Instant.class) {{ Instant.now(); result = testInstant; }};

      Instant now = Instant.now();

      assertSame(testInstant, now);
   }
}

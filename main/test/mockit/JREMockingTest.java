package mockit;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.jar.*;
import java.util.logging.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class JREMockingTest
{
   // Mocking java.io.File ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void mockingOfFile(@Mocked final File file) {
      new Expectations() {{
         file.exists(); result = true;
      }};

      File f = new File("...");
      assertTrue(f.exists());
   }

   @Test
   public void mockingFileAndRecordingExpectationToMatchOnSpecificConstructorCall(@Mocked File anyFile) {
      new Expectations() {{
         new File("a.txt").exists(); result = true;
      }};

      boolean aExists = new File("a.txt").exists();
      //noinspection TooBroadScope
      boolean bExists = new File("b.txt").exists();

      assertTrue(aExists);
      assertFalse(bExists);
   }

   // Faking java.util.Calendar ///////////////////////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void fakingOfCalendar() {
      final Calendar calCST = new GregorianCalendar(2010, Calendar.MAY, 15);
      final TimeZone tzCST = TimeZone.getTimeZone("CST");
      new MockUp<Calendar>() {
         @Mock Calendar getInstance(Invocation inv, TimeZone tz) { return tz == tzCST ? calCST : inv.<Calendar>proceed(); }
      };

      Calendar cal1 = Calendar.getInstance(tzCST);
      assertSame(calCST, cal1);
      assertEquals(2010, cal1.get(Calendar.YEAR));

      TimeZone tzPST = TimeZone.getTimeZone("PST");
      Calendar cal2 = Calendar.getInstance(tzPST);
      assertNotSame(calCST, cal2);
   }

   // Mocking java.util.Date //////////////////////////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void regularMockingOfAnnotatedJREMethod(@Mocked Date d) throws Exception {
      assertTrue(d.getClass().getDeclaredMethod("parse", String.class).isAnnotationPresent(Deprecated.class));
   }

   @Test @SuppressWarnings("deprecation")
   public void dynamicMockingOfAnnotatedJREMethod() throws Exception {
      final Date d = new Date();

      new Expectations(d) {{
         d.getMinutes(); result = 5;
      }};

      assertEquals(5, d.getMinutes());
      assertTrue(Date.class.getDeclaredMethod("getMinutes").isAnnotationPresent(Deprecated.class));
   }

   // Mocking of IO classes ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

   @Injectable FileOutputStream stream;
   @Injectable Writer writer;

   @Test
   public void dynamicMockingOfFileOutputStreamThroughMockField() throws Exception {
      new Expectations() {{
         //noinspection ConstantConditions
         stream.write((byte[]) any);
      }};

      stream.write("Hello world".getBytes());
      writer.append('x');

      new Verifications() {{ writer.append('x'); }};
   }

   // Un-mockable JRE classes /////////////////////////////////////////////////////////////////////////////////////////////////////////////

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREClass(@Mocked StringBuffer unmockable) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREClass(@Mocked StringBuilder unmockable) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREClass(@Mocked ArrayList<?> unmockable) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREClass(@Mocked LinkedList<?> unmockable) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREClass(@Mocked HashMap<?, ?> unmockable) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREClass(@Mocked HashSet<?> unmockable) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREClass(@Mocked AbstractSet<?> unmockable) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREClass(@Mocked Hashtable<?, ?> unmockable) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREClass(@Mocked Properties unmockable) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREClass(@Mocked Exception unmockable) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREClass(@Mocked Throwable unmockable) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREClass(@Mocked Thread unmockable) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREClass(@Mocked ThreadLocal<?> unmockable) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREClass(@Mocked ClassLoader unmockable) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREClass(@Mocked Class<?> unmockable) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREClass(@Mocked Math unmockable) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREClass(@Mocked StrictMath unmockable) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREClass(@Mocked Object unmockable) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREClass(@Mocked Enum<?> unmockable) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREClass(@Mocked System unmockable) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREClass(@Mocked JarFile unmockable) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREClass(@Mocked JarEntry unmockable) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREClass(@Mocked Manifest unmockable) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREClass(@Mocked Attributes unmockable) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREClass(@SuppressWarnings("Since15") @Mocked Duration unmockable) { fail("Should never get here"); }

   // Un-mockable JRE interfaces //////////////////////////////////////////////////////////////////////////////////////////////////////////

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREInterface(@Mocked Collection<?> mockCol) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREInterface(@Mocked List<?> mockList) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREInterface(@Mocked Set<?> mockSet) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREInterface(@Injectable SortedSet<?> mockSortedSet) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREInterface(@Mocked Map<?, ?> mockMap) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREInterface(@Capturing SortedMap<?, ?> mockSortedMap) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREInterface(@Mocked Comparator<?> mockComparator) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREInterface(@Mocked Queue<?> mockQueue) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREInterface(@Mocked Enumeration<?> mockEnumeration) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREInterface(@Mocked Iterator<?> mockIterator) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockUnmockableJREInterface(@Mocked Map.Entry<?, ?> mockMapEntry) { fail("Should never get here"); }

   // Mocking java.time ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   public interface DurationProvider { @SuppressWarnings("Since15") Duration getDuration(); }

   @Test
   public void mockMethodWhichReturnsADuration(@Mocked DurationProvider mock) {
      Object d = mock.getDuration();

      assertNull(d);
   }

   // Mocking java.util.logging ///////////////////////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void mockLogManager(@Mocked LogManager mock) {
      LogManager logManager = LogManager.getLogManager();
      //noinspection MisorderedAssertEqualsArguments
      assertSame(mock, logManager);
   }

   @Test
   public void mockLogger(@Mocked Logger mock) {
      // TODO: this call causes Surefire to fail: assertNotNull(LogManager.getLogManager());
      //noinspection MisorderedAssertEqualsArguments
      assertSame(mock, Logger.getLogger("test"));
   }
}
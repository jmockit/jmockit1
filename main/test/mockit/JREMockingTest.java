package mockit;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.jar.*;
import java.util.logging.*;

import org.junit.*;
import static org.junit.Assert.*;

import static mockit.internal.util.Utilities.JAVA8;

public final class JREMockingTest
{
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

   @Test
   public void mockingOfCalendar() {
      final Calendar calCST = new GregorianCalendar(2010, Calendar.MAY, 15);
      final TimeZone tzCST = TimeZone.getTimeZone("CST");
      new Expectations(Calendar.class) {{ Calendar.getInstance(tzCST); result = calCST; }};

      Calendar cal1 = Calendar.getInstance(tzCST);
      assertSame(calCST, cal1);
      assertEquals(2010, cal1.get(Calendar.YEAR));

      TimeZone tzPST = TimeZone.getTimeZone("PST");
      Calendar cal2 = Calendar.getInstance(tzPST);
      assertNotSame(calCST, cal2);
   }

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

   @Test
   public void attemptToMockJREClassThatIsNeverMockable() {
      attemptToMockUnmockableJREClass(String.class);
      attemptToMockUnmockableJREClass(StringBuffer.class);
      attemptToMockUnmockableJREClass(StringBuilder.class);
      attemptToMockUnmockableJREClass(ArrayList.class);
      attemptToMockUnmockableJREClass(LinkedList.class);
      attemptToMockUnmockableJREClass(HashMap.class);
      attemptToMockUnmockableJREClass(HashSet.class);
      attemptToMockUnmockableJREClass(Hashtable.class);
      attemptToMockUnmockableJREClass(Properties.class);
      attemptToMockUnmockableJREClass(AbstractSet.class);
      attemptToMockUnmockableJREClass(Exception.class);
      attemptToMockUnmockableJREClass(Throwable.class);
      attemptToMockUnmockableJREClass(Thread.class);
      attemptToMockUnmockableJREClass(ThreadLocal.class);
      attemptToMockUnmockableJREClass(ClassLoader.class);
      attemptToMockUnmockableJREClass(Class.class);
      attemptToMockUnmockableJREClass(Math.class);
      attemptToMockUnmockableJREClass(StrictMath.class);
      attemptToMockUnmockableJREClass(Object.class);
      attemptToMockUnmockableJREClass(Enum.class);
      attemptToMockUnmockableJREClass(System.class);
      attemptToMockUnmockableJREClass(JarFile.class);
      attemptToMockUnmockableJREClass(JarEntry.class);
      attemptToMockUnmockableJREClass(Manifest.class);
      attemptToMockUnmockableJREClass(Attributes.class);

      if (JAVA8) {
         //noinspection Since15
         attemptToMockUnmockableJREClass(Duration.class);
      }
   }

   void attemptToMockUnmockableJREClass(Class<?> jreClass) {
      try {
         new Expectations(jreClass) {};
         fail("Allowed mocking of " + jreClass);
      }
      catch (IllegalArgumentException e) {
         String msg = e.getMessage();
         assertTrue(msg.contains(jreClass.getName()) || msg.endsWith("is not mockable"));
      }
   }

   // Un-mockable JRE interfaces //////////////////////////////////////////////////////////////////////////////////////////////////////////

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockJREInterface(@Mocked Collection<?> mockCol) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockJREInterface(@Mocked List<?> mockList) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockJREInterface(@Mocked Set<?> mockSet) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockJREInterface(@Injectable SortedSet<?> mockSortedSet) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockJREInterface(@Mocked Map<?, ?> mockMap) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockJREInterface(@Capturing SortedMap<?, ?> mockSortedMap) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockJREInterface(@Mocked Comparator<?> mockComparator) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockJREInterface(@Mocked Queue<?> mockQueue) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockJREInterface(@Mocked Enumeration<?> mockEnumeration) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockJREInterface(@Mocked Iterator<?> mockIterator) { fail("Should never get here"); }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockJREInterface(@Mocked Map.Entry<?, ?> mockMapEntry) { fail("Should never get here"); }

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

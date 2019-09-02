package mockit;

import java.io.*;
import java.net.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class ClassLoadingAndJREMocksTest
{
   static class Foo {}

   @Test
   public void fakeFile() {
      new MockUp<File>() {
         @Mock void $init(String name) {} // not necessary, except to verify non-occurrence of NPE
         @Mock boolean exists() { return true; }
      };

      new Foo(); // causes a class load
      assertTrue(new File("filePath").exists());
   }

   @Test
   public void fakeFileSafelyUsingReentrantFakeMethod() {
      new MockUp<File>() {
         @Mock
         boolean exists(Invocation inv)
         {
            File it = inv.getInvokedInstance();
            return "testFile".equals(it.getName()) || inv.<Boolean>proceed();
         }
      };

      checkForTheExistenceOfSeveralFiles();
   }

   void checkForTheExistenceOfSeveralFiles() {
      assertFalse(new File("someOtherFile").exists());
      assertTrue(new File("testFile").exists());
      assertFalse(new File("yet/another/file").exists());
      assertTrue(new File("testFile").exists());
   }

   @Test
   public void fakeFileSafelyUsingProceed() {
      new MockUp<File>() {
         @Mock
         boolean exists(Invocation inv)
         {
            File it = inv.getInvokedInstance();
            return "testFile".equals(it.getName()) || inv.<Boolean>proceed();
         }
      };

      checkForTheExistenceOfSeveralFiles();
   }

   @Test
   public void attemptToMockNonMockableJREClass(@Mocked Integer mock) {
      assertNull(mock);
   }

   @Test
   public void mockURLAndURLConnection(@Mocked URL mockUrl, @Mocked URLConnection mockConnection) throws Exception {
      URLConnection conn = mockUrl.openConnection();

      assertSame(mockConnection, conn);
   }

   @Test
   public void mockURLAndHttpURLConnection(
      @Mocked URL mockUrl, @Mocked HttpURLConnection mockConnection
   ) throws Exception {
      HttpURLConnection conn = (HttpURLConnection) mockUrl.openConnection();
      assertSame(mockConnection, conn);
   }

   @Test
   public void mockURLAndHttpURLConnectionWithDynamicMock(@Mocked final HttpURLConnection mockHttpConnection) throws Exception {
      final URL url = new URL("http://nowhere");

      new Expectations(url) {{
         url.openConnection(); result = mockHttpConnection;
         mockHttpConnection.getOutputStream(); result = new ByteArrayOutputStream();
      }};

      // Code under test:
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setDoOutput(true);
      conn.setRequestMethod("PUT");
      OutputStream out = conn.getOutputStream();

      assertNotNull(out);

      new Verifications() {{
         mockHttpConnection.setDoOutput(true);
         mockHttpConnection.setRequestMethod("PUT");
      }};
   }

   String readResourceContent() throws IOException {
      URL url = new URL("http://remoteHost/aResource");
      URLConnection connection = url.openConnection();

      connection.setConnectTimeout(1000);
      connection.connect();

      return connection.getContent().toString();
   }

   @Test
   public void cascadingMockedURLWithInjectableCascadedURLConnection(
      @Mocked URL anyUrl, @Injectable final URLConnection cascadedUrlConnection
   ) throws Exception {
      String testContent = recordURLConnectionToReturnContent(cascadedUrlConnection);

      String content = readResourceContent();

      assertEquals(testContent, content);
   }

   String recordURLConnectionToReturnContent(final URLConnection urlConnection) throws IOException {
      final String testContent = "testing";
      new Expectations() {{ urlConnection.getContent(); result = testContent; }};
      return testContent;
   }

   @Test
   public void fakeURLUsingInjectableURLConnection(@Injectable final URLConnection urlConnection) throws Exception {
      final String testContent = recordURLConnectionToReturnContent(urlConnection);
      new MockUp<URL>() {
         @Mock void $init(URL context, String spec, URLStreamHandler handler) {}
         @Mock URLConnection openConnection() { return urlConnection; }
      };

      String content = readResourceContent();

      assertEquals(testContent, content);
   }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockJREClassThatIsNeverMockable(@Mocked Class<?> mockClass) {}
}
package integrationTests;

import java.io.*;
import javax.xml.stream.*;

import org.junit.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import mockit.coverage.*;

import integrationTests.ClassInRegularPackage.*;
import static javax.xml.stream.XMLStreamConstants.*;

public final class XmlFileTest
{
   final File xmlFile = new File(Configuration.getOrChooseOutputDirectory(""), "coverage.xml");
   XMLStreamReader xmlReader;

   @Test
   public void generateXmlFileWithCoverageData() throws Exception {
      assumeTrue("xml".equals(System.getProperty("coverage-output")));
      assumeTrue("loaded".equals(System.getProperty("coverage-classes")));
      assumeTrue("src".equals(System.getProperty("coverage-srcDirs")));

      new ClassInRegularPackage().doSomething(NestedEnum.Second);
      CodeCoverage.generateOutput();

      try (InputStream xmlFileContents = new FileInputStream(xmlFile)) {
         xmlReader = XMLInputFactory.newFactory().createXMLStreamReader(xmlFileContents);

         assertEquals("UTF-8", xmlReader.getCharacterEncodingScheme());

         // <coverage version="1">
         assertEquals(START_ELEMENT, xmlReader.nextTag());
         assertEquals("coverage", xmlReader.getLocalName());
         assertEquals("1", xmlReader.getAttributeValue(0));

         // <file path="...">
         assertEquals(START_ELEMENT, xmlReader.nextTag());
         assertEquals("file", xmlReader.getLocalName());
         assertEquals(1, xmlReader.getAttributeCount());
         assertEquals("path", xmlReader.getAttributeLocalName(0));
         assertEquals("src/integrationTests/ClassInRegularPackage.java", xmlReader.getAttributeValue(0));

         assertLineToCover(3, true);
         assertLineToCover(7, true);
         assertLineToCover(10, true);
         assertLineToCover(13, true);
         assertLineToCover(24, true);
         assertLineToCover(26, false);
         assertLineToCover(29, true);
         assertLineToCover(33, true, 2, 1);

         // </file>
         assertEquals(END_ELEMENT, xmlReader.nextTag());
         assertEquals("file", xmlReader.getLocalName());

         // </coverage>
         assertEquals(END_ELEMENT, xmlReader.nextTag());
         assertEquals("coverage", xmlReader.getLocalName());
      }
   }

   // <lineToCover lineNumber="n" covered="true|false"/>
   void assertLineToCover(int lineNumber, boolean covered) throws Exception {
      assertLineToCover(lineNumber, covered, 0, 0);
   }

   // <lineToCover lineNumber="n" covered="true|false" branchesToCover="n" coveredBranches="n"/>
   void assertLineToCover(int lineNumber, boolean covered, int branches, int coveredBranches) throws Exception {
      assertEquals(START_ELEMENT, xmlReader.nextTag());
      assertEquals("lineToCover", xmlReader.getLocalName());
      assertEquals(branches == 0 ? 2 : 4, xmlReader.getAttributeCount());

      int attributeIndex = 0;
      assertEquals("lineNumber", xmlReader.getAttributeLocalName(attributeIndex));
      assertEquals(Integer.toString(lineNumber), xmlReader.getAttributeValue(attributeIndex));

      attributeIndex++;
      assertEquals("covered", xmlReader.getAttributeLocalName(attributeIndex));
      assertEquals(Boolean.toString(covered), xmlReader.getAttributeValue(attributeIndex));

      if (branches > 0) {
         attributeIndex++;
         assertEquals("branchesToCover", xmlReader.getAttributeLocalName(attributeIndex));
         assertEquals(Integer.toString(branches), xmlReader.getAttributeValue(attributeIndex));

         attributeIndex++;
         assertEquals("coveredBranches", xmlReader.getAttributeLocalName(attributeIndex));
         assertEquals(Integer.toString(coveredBranches), xmlReader.getAttributeValue(attributeIndex));
      }

      assertEquals(END_ELEMENT, xmlReader.nextTag());
   }

   @After
   public void deleteGeneratedXmlFile() {
      xmlFile.delete();
   }
}

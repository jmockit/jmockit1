package integrationTests;

import integrationTests.data.ClassWithFields;
import mockit.coverage.CodeCoverage;
import mockit.coverage.Configuration;
import mockit.coverage.XmlFile;
import mockit.coverage.data.CoverageData;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.HandlerBase;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.*;
import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

public class CoverageXmlTest extends CoverageTest
{
   ClassWithFields tested;

   @Test
   public void verifyXmlOutputGenerator()
   {
      File outputFile = new File("target", "coverage.xml");
      if (outputFile.exists()) {
         outputFile.delete();
      }
      assumeThat(Configuration.getProperty("output"), is(equalTo("xml")));
      assumeThat(Configuration.getProperty("outputDir"), is(equalTo("target")));

      CodeCoverage.generateOutput(false);

      assertTrue(outputFile.exists());

      outputFile.delete();
   }

   @Test
   public void verifyXmlFile() throws IOException
   {
      File outputFile = File.createTempFile("coverage_", ".xml", new File("target"));
      outputFile.deleteOnExit();

      new XmlFile("target", CoverageData.instance()).generate();

      assertTrue(outputFile.exists());
   }
}

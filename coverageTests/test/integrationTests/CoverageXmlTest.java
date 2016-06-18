package integrationTests;

import integrationTests.data.ClassWithFields;
import mockit.coverage.CodeCoverage;
import mockit.coverage.Configuration;
import mockit.coverage.XmlFile;
import mockit.coverage.data.CoverageData;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

public class CoverageXmlTest extends CoverageTest {
   ClassWithFields tested;

   @Test
   public void verifyXmlOutputGenerator() {
      assumeThat(Configuration.getProperty("output"), is(equalTo("xml")));
      assumeThat(Configuration.getProperty("outputDir"), is(equalTo("target")));
      CodeCoverage.generateOutput(false);

      File outputFile = new File("target", "coverage.xml");
      assertTrue(outputFile.exists());
   }

   @Test
   public void verifyXmlFile() throws IOException {
      File outputFile = File.createTempFile("coverage_", ".xml", new File("target"));
      outputFile.deleteOnExit();

      new XmlFile("target", CoverageData.instance()).generate();

      assertTrue(outputFile.exists());
   }
}

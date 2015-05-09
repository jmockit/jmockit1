package mockit.coverage.reporting;

import java.io.File;
import java.io.IOException;

import mockit.coverage.data.CoverageData;

import org.junit.Before;
import org.junit.Test;

public class XmlCoverageReportTest
{
   private CoverageData coverageData;

   @Before
   public void readData() throws IOException {
      coverageData = CoverageData.readDataFromFile(new File("src/mockit/coverage/reporting/testcoverage.ser"));
   }
   
   @Test
   public void xmlReport() throws IOException {
      
      XmlCoverageReport report = new XmlCoverageReport("target/", coverageData);
      
      report.generate();
   }
}

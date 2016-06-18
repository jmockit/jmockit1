package mockit.coverage;

import mockit.coverage.data.CoverageData;
import mockit.coverage.data.FileCoverageData;
import mockit.coverage.lines.LineCoverageData;
import mockit.coverage.lines.PerFileLineCoverage;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

/**
 * enerates a xml file for Sonar Cube's Generic Test Coverage.
 * http://docs.sonarqube.org/display/SONAR/Generic+Test+Coverage
 */
public class XmlFile
{
   @Nonnull private final File outputFile;
   @Nonnull private final CoverageData coverageData;

	private static final String XML_FORMAT_VERSION="1";

   public XmlFile(String outputDir, CoverageData coverageData)
	{
      this.outputFile = new File(outputDir.isEmpty() ? null: outputDir, "coverage.xml");

		coverageData.fillLastModifiedTimesForAllClassFiles();
      this.coverageData = coverageData;
	}
   
   /** Generates a report of the format:
    * <pre>
    * &lt;coverage version="1">
    *    &lt;file path="src/main/java/com/example/MyClass.java">
    *       &lt;lineToCover lineNumber="2" covered="false"/>
    *       &lt;lineToCover lineNumber="3" covered="true" branchesToCover="8" coveredBranches="7"/>
    *    &lt;/file>
    * &lt;/coverage>
    * </pre>
    * TODO branch coverage is not yet implemented
    */
   public void generate() throws IOException
	{
	   outputFile.getAbsoluteFile().getParentFile().mkdirs();
	   PrintStream xmlOut = null;
	   try {
	      xmlOut = new PrintStream(new FileOutputStream(outputFile), true, "UTF-8");
			xmlOut.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
	      xmlOut.printf("<coverage version=\"%s\">\n", XML_FORMAT_VERSION);
	      for (Map.Entry<String, FileCoverageData> entry : this.coverageData.getFileToFileDataMap().entrySet()) {
	         xmlOut.printf("<file path=\"src/%s\">\n", entry.getKey());
	         PerFileLineCoverage lcinfo = entry.getValue().lineCoverageInfo;
	         int maxlines = lcinfo.getLineCount();
	         for (int i = 0; i <= maxlines; i++) {
	            if (lcinfo.hasLineData(i)) {
   	            LineCoverageData lineData = lcinfo.getLineData(i);
   	            if (!lineData.isEmpty()) {
   	               xmlOut.printf("<lineToCover lineNumber=\"%d\" covered=\"%b\"/>\n", i, lineData.isCovered());
   	            }
	            }
	         }
	         xmlOut.println("</file>");
	      }
	      xmlOut.println("</coverage>");
	      System.out.println("JMockit: Coverage report written to " + outputFile.getCanonicalPath());
	   } finally {
	      if (xmlOut != null) {
	         xmlOut.close();
	      }
	   }
	}
}

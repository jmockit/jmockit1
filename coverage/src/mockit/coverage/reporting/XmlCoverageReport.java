package mockit.coverage.reporting;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

import mockit.coverage.data.CoverageData;
import mockit.coverage.data.FileCoverageData;
import mockit.coverage.lines.LineCoverageData;
import mockit.coverage.lines.PerFileLineCoverage;

/**
 *  http://docs.sonarqube.org/display/SONAR/Generic+Test+Coverage
 */
public class XmlCoverageReport
{

	private final String outputDir;
   private final CoverageData coverageData;

   public XmlCoverageReport(String outputDir, CoverageData coverageData) 
	{
	   this.outputDir = CoverageReport.getOrChooseOutputDirectory(outputDir);
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
	   new File(outputDir).mkdirs();
	   File coverageFile = new File(outputDir, "coverage.xml");
	   PrintStream xmlOut = null;
	   try {
	      xmlOut = new PrintStream(new FileOutputStream(coverageFile), true, "UTF-8");
	      xmlOut.println("<coverage version=\"1\">");
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
	      System.out.println("JMockit: Coverage report written to " + coverageFile.getCanonicalPath());
	   } finally {
	      if (xmlOut != null) {
	         xmlOut.close();
	      }
	   }
	}
}

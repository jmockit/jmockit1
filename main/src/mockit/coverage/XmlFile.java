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
import java.util.List;
import java.util.Arrays;

/**
 * Generates an xml file for Sonar Cube's Generic Test Coverage.
 * http://docs.sonarqube.org/display/SONAR/Generic+Test+Coverage
 */
public class XmlFile
{
   private static final String XML_FORMAT_VERSION="1";

   @Nonnull private final File outputFile;
   @Nonnull private final CoverageData coverageData;
   private final List<String> srcDirs;

   /**
    * This constructor is used by the OutputFileGenerator.
    */
   public XmlFile(String outputDir, CoverageData coverageData)
	{
      this(new File(outputDir.isEmpty() ? null: outputDir, "coverage.xml"), null, coverageData);
	}

   /**
    * This constructor can be used in test cases.
    */
   protected XmlFile(File outputFile, List<String> srcDirs, CoverageData coverageData)
   {
      this.outputFile = outputFile;
      this.srcDirs = srcDirs == null ? sourceDirectories() : srcDirs;
      coverageData.fillLastModifiedTimesForAllClassFiles();
      this.coverageData = coverageData;
   }

   private static List<String> sourceDirectories()
   {
      String srcDirs = Configuration.getProperty("srcDirs");
      if (srcDirs == null) {
         return Arrays.asList(System.getProperty("user.dir"));
      }
      else {
         List<String> srcDirList = Arrays.asList(srcDirs.split("\\s*,\\s*"));
         for (String dir : srcDirList) {
            File d = new File(dir);
            if (!d.getAbsoluteFile().isDirectory()) {
               throw new IllegalArgumentException("Directory " + dir + " doesn't exist");
            }
         }
         return srcDirList;
      }
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
            String srcFile = findSrcFile(entry.getKey());
	         xmlOut.printf("<file path=\"%s\">\n", srcFile);
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

   private String findSrcFile(String file) {
      for (String srcDir : srcDirs) {
         File f = new File(srcDir, file);
         if (f.exists()) {
            return f.getPath();
         }
      }
      return file;
   }
}

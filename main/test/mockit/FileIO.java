/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;

public final class FileIO
{
   public void writeToFile(String fileName) throws IOException
   {
      FileWriter writer = new FileWriter(fileName);
      Writer out = new BufferedWriter(writer);

      try {
         out.write("Test FileIO");
      }
      finally {
         out.close();
      }

      System.out.println("File written");
   }
}

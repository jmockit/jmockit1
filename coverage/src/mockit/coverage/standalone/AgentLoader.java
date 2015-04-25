/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.standalone;

import java.io.*;
import java.net.*;
import java.security.*;
import javax.annotation.*;

import com.sun.tools.attach.*;

public final class AgentLoader
{
   private static final float JAVA_VERSION = Float.parseFloat(System.getProperty("java.specification.version"));
   @Nonnull private final String processIdForTargetVM;

   public AgentLoader(@Nonnull String processIdForTargetVM)
   {
      if (JAVA_VERSION < 1.6F) {
         throw new IllegalStateException("JMockit Coverage requires a Java 6+ VM");
      }

      this.processIdForTargetVM = processIdForTargetVM;
   }

   public void loadAgent()
   {
      try {
         VirtualMachine vm = VirtualMachine.attach(processIdForTargetVM);
         String jarFilePath = getPathToJarFile();
         vm.loadAgent(jarFilePath, null); // TODO: use "options" parameter to pass "coverage-xyz" system properties
         // TODO: maybe use vm.getAgentProperties() to obtain port number for server socket opened inside target VM
         vm.detach();
      }
      catch (AttachNotSupportedException e) { throw new RuntimeException(e); }
      catch (AgentLoadException e) { throw new IllegalStateException(e); }
      catch (AgentInitializationException e) { throw new IllegalStateException(e); }
      catch (IOException e) { throw new RuntimeException(e); }
   }

   @Nonnull
   private String getPathToJarFile()
   {
      CodeSource codeSource = getClass().getProtectionDomain().getCodeSource();
      URL location = codeSource.getLocation();

      // URI is used to deal with spaces and non-ASCII characters.
      URI jarFileURI;
      try { jarFileURI = location.toURI(); } catch (URISyntaxException e) { throw new RuntimeException(e); }

      // Certain environments (JBoss) use something other than "file:", which is not accepted by File.
      if (!"file".equals(jarFileURI.getScheme())) {
         String locationPath = location.toExternalForm();
         int p = locationPath.indexOf(':');
         return locationPath.substring(p + 2);
      }

      return new File(jarFileURI).getPath();
   }
}

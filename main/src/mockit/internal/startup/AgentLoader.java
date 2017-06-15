/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.startup;

import java.io.*;
import java.lang.management.*;
import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;

import static mockit.internal.util.Utilities.*;

import com.sun.tools.attach.*;
import com.sun.tools.attach.spi.*;
import sun.tools.attach.*;

public final class AgentLoader
{
   private static final AttachProvider ATTACH_PROVIDER = new AttachProvider() {
      @Override @Nullable public String name() { return null; }
      @Override @Nullable public String type() { return null; }
      @Override @Nullable public VirtualMachine attachVirtualMachine(String id) { return null; }
      @Override @Nullable public List<VirtualMachineDescriptor> listVirtualMachines() { return null; }
   };

   @Nonnull private final String jarFilePath;
   @Nullable private String pidForTargetVM;

   AgentLoader()
   {
      if (JAVA_VERSION < 1.6F) {
         throw new IllegalStateException("JMockit requires a Java 6+ VM");
      }

      jarFilePath = PathToAgentJar.getPathToJarFile();
   }

   public AgentLoader(@Nonnull String pid)
   {
      this();
      pidForTargetVM = pid;
   }

   public void loadAgent(@Nullable String options)
   {
      VirtualMachine vm;

      if (AttachProvider.providers().isEmpty()) {
         if (HOTSPOT_VM) {
            vm = getVirtualMachineImplementationFromEmbeddedOnes();
         }
         else {
            String helpMessage = getHelpMessageForNonHotSpotVM();
            throw new IllegalStateException(helpMessage);
         }
      }
      else {
         vm = attachToRunningVM();
      }

      loadAgentAndDetachFromRunningVM(vm, options);
   }

   @Nonnull
   private VirtualMachine getVirtualMachineImplementationFromEmbeddedOnes()
   {
      Class<? extends VirtualMachine> vmClass = findVirtualMachineClassAccordingToOS();
      Class<?>[] parameterTypes = {AttachProvider.class, String.class};
      String pid = getProcessIdForTargetVM();

      try {
         // This is only done with Reflection to avoid the JVM pre-loading all the XyzVirtualMachine classes.
         Constructor<? extends VirtualMachine> vmConstructor = vmClass.getConstructor(parameterTypes);
         VirtualMachine newVM = vmConstructor.newInstance(ATTACH_PROVIDER, pid);
         return newVM;
      }
      catch (NoSuchMethodException e)     { throw new RuntimeException(e); }
      catch (InvocationTargetException e) { throw new RuntimeException(e); }
      catch (InstantiationException e)    { throw new RuntimeException(e); }
      catch (IllegalAccessException e)    { throw new RuntimeException(e); }
      catch (NoClassDefFoundError e) {
         throw new IllegalStateException("Native library for Attach API not available in this JRE", e);
      }
      catch (UnsatisfiedLinkError e) {
         throw new IllegalStateException("Native library for Attach API not available in this JRE", e);
      }
   }

   @Nonnull
   private static Class<? extends VirtualMachine> findVirtualMachineClassAccordingToOS()
   {
      if (File.separatorChar == '\\') {
         return WindowsVirtualMachine.class;
      }

      String osName = System.getProperty("os.name");

      if (osName.startsWith("Linux") || osName.startsWith("LINUX")) {
         return LinuxVirtualMachine.class;
      }

      if (osName.contains("FreeBSD") || osName.startsWith("Mac OS X")) {
         return BsdVirtualMachine.class;
      }

      if (osName.startsWith("Solaris") || osName.contains("SunOS")) {
         return SolarisVirtualMachine.class;
      }

      throw new IllegalStateException("Cannot use Attach API on unknown OS: " + osName);
   }

   @Nonnull
   private String getProcessIdForTargetVM()
   {
      if (pidForTargetVM != null) {
         return pidForTargetVM;
      }

      String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
      int p = nameOfRunningVM.indexOf('@');
      return nameOfRunningVM.substring(0, p);
   }

   @Nonnull
   private String getHelpMessageForNonHotSpotVM()
   {
      String vmName = System.getProperty("java.vm.name");
      String helpMessage = "To run on " + vmName;

      if (vmName.contains("J9")) {
         helpMessage += ", add <IBM SDK>/lib/tools.jar to the runtime classpath (before jmockit), or";
      }

      return helpMessage + " use -javaagent:" + jarFilePath;
   }

   @Nonnull
   private VirtualMachine attachToRunningVM()
   {
      String pid = getProcessIdForTargetVM();

      try {
         return VirtualMachine.attach(pid);
      }
      catch (AttachNotSupportedException e) { throw new RuntimeException(e); }
      catch (IOException e) {
         if (e.getMessage().contains("current VM")) {
            throw new IllegalStateException(
               "Running on JDK 9 requires -javaagent:<proper path>/jmockit-1.n.jar or -Djdk.attach.allowAttachSelf");
         }

         throw new RuntimeException(e);
      }
   }

   private void loadAgentAndDetachFromRunningVM(@Nonnull VirtualMachine vm, @Nullable String options)
   {
      try {
         vm.loadAgent(jarFilePath, options);
         vm.detach();
      }
      catch (AgentLoadException e) { throw new IllegalStateException(e); }
      catch (AgentInitializationException e) { throw new IllegalStateException(e); }
      catch (IOException e) { throw new RuntimeException(e); }
   }
}

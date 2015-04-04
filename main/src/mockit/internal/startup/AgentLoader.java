/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.startup;

import java.io.*;
import java.lang.management.*;
import java.util.*;

import mockit.internal.util.*;

import com.sun.tools.attach.*;
import com.sun.tools.attach.spi.*;
import org.jetbrains.annotations.*;
import sun.tools.attach.*;

final class AgentLoader
{
   private static final AttachProvider ATTACH_PROVIDER = new AttachProvider() {
      @Override @Nullable public String name() { return null; }
      @Override @Nullable public String type() { return null; }
      @Override @Nullable public VirtualMachine attachVirtualMachine(String id) { return null; }
      @Override @Nullable public List<VirtualMachineDescriptor> listVirtualMachines() { return null; }
   };

   @NotNull private final String jarFilePath;
   AgentLoader(@NotNull String jarFilePath) { this.jarFilePath = jarFilePath; }

   void loadAgent()
   {
      VirtualMachine vm;

      if (AttachProvider.providers().isEmpty()) {
         String vmName = System.getProperty("java.vm.name");

         if (vmName.contains("HotSpot")) {
            vm = getVirtualMachineImplementationFromEmbeddedOnes();
         }
         else {
            String helpMessage = getHelpMessageForNonHotSpotVM(vmName);
            throw new IllegalStateException(helpMessage);
         }
      }
      else {
         vm = attachToThisVM();
      }

      loadAgentAndDetachFromThisVM(vm);
   }

   @NotNull
   private static VirtualMachine getVirtualMachineImplementationFromEmbeddedOnes()
   {
      Class<? extends VirtualMachine> vmClass = findVirtualMachineClassAccordingToOS();
      Class<?>[] parameterTypes = {AttachProvider.class, String.class};
      String pid = discoverProcessIdForRunningVM();

      try {
         // This is only done with Reflection to avoid the JVM pre-loading all the XyzVirtualMachine classes.
         VirtualMachine newVM = ConstructorReflection.newInstance(vmClass, parameterTypes, ATTACH_PROVIDER, pid);
         return newVM;
      }
      catch (UnsatisfiedLinkError e) {
         throw new IllegalStateException("Native library for Attach API not available in this JRE", e);
      }
   }

   @NotNull
   private static Class<? extends VirtualMachine> findVirtualMachineClassAccordingToOS()
   {
      if (File.separatorChar == '\\') {
         return WindowsVirtualMachine.class;
      }

      String osName = System.getProperty("os.name");

      if (osName.startsWith("Linux") || osName.startsWith("LINUX")) {
         return LinuxVirtualMachine.class;
      }
      else if (osName.startsWith("Mac OS X")) {
         return BsdVirtualMachine.class;
      }
      else if (osName.startsWith("Solaris")) {
         return SolarisVirtualMachine.class;
      }

      throw new IllegalStateException("Cannot use Attach API on unknown OS: " + osName);
   }

   @NotNull
   private static String discoverProcessIdForRunningVM()
   {
      String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
      int p = nameOfRunningVM.indexOf('@');

      return nameOfRunningVM.substring(0, p);
   }

   @NotNull
   private static String getHelpMessageForNonHotSpotVM(@NotNull String vmName)
   {
      String helpMessage = "To run on " + vmName;

      if (vmName.contains("J9")) {
         helpMessage += ", add <IBM SDK>/lib/tools.jar to the runtime classpath (before jmockit), or";
      }

      return helpMessage + " use -javaagent:<proper path>/jmockit.jar";
   }

   @NotNull
   private static VirtualMachine attachToThisVM()
   {
      String pid = discoverProcessIdForRunningVM();

      try {
         return VirtualMachine.attach(pid);
      }
      catch (AttachNotSupportedException e) {
         throw new RuntimeException(e);
      }
      catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private void loadAgentAndDetachFromThisVM(@NotNull VirtualMachine vm)
   {
      try {
         vm.loadAgent(jarFilePath, null);
         vm.detach();
      }
      catch (AgentLoadException e) {
         throw new IllegalStateException(e);
      }
      catch (AgentInitializationException e) {
         throw new IllegalStateException(e);
      }
      catch (IOException e) {
         throw new RuntimeException(e);
      }
   }
}

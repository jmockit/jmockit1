/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.startup;

import java.io.*;
import java.lang.management.*;
import java.lang.reflect.*;
import java.net.*;
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

   AgentLoader() {
      if (JAVA_VERSION < 1.7F) {
         throw new IllegalStateException("JMockit requires a Java 7+ VM");
      }

      String currentPath = "";
      try { currentPath = getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath(); }
      catch (URISyntaxException ignore) {}

      if (currentPath.endsWith(".jar")) {
         currentPath = new File(currentPath).getAbsolutePath();
      }
      else {
         int p = currentPath.lastIndexOf("/main/target/classes");
         currentPath = currentPath.substring(0, p);
         currentPath = new File(currentPath, "agent.jar").getPath();
      }

      jarFilePath = currentPath;
   }

   public AgentLoader(@Nonnull String pid) {
      this();
      pidForTargetVM = pid;
   }

   public void loadAgent(@Nullable String options) {
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
   private VirtualMachine getVirtualMachineImplementationFromEmbeddedOnes() {
      Class<? extends VirtualMachine> vmClass = findVirtualMachineClassAccordingToOS();
      String pid = getProcessIdForTargetVM();

      try {
         // This is only done with Reflection to avoid the JVM pre-loading all the XyzVirtualMachine classes.
         Class<?>[] parameterTypes = {AttachProvider.class, String.class};
         Constructor<? extends VirtualMachine> vmConstructor = vmClass.getConstructor(parameterTypes);
         VirtualMachine newVM = vmConstructor.newInstance(ATTACH_PROVIDER, pid);
         return newVM;
      }
      catch (NoSuchMethodException | InvocationTargetException | InstantiationException | NoClassDefFoundError | IllegalAccessException e) {
         throw new RuntimeException(e);
      }
      catch (UnsatisfiedLinkError e) {
         throw new IllegalStateException("Native library for Attach API not available in this JRE", e);
      }
   }

   @Nonnull
   private static Class<? extends VirtualMachine> findVirtualMachineClassAccordingToOS() {
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

      if (osName.contains("AIX")) {
         return AixVirtualMachine.class;
      }

      throw new IllegalStateException("Cannot use Attach API on unknown OS: " + osName);
   }

   @Nonnull
   private String getProcessIdForTargetVM() {
      if (pidForTargetVM != null) {
         return pidForTargetVM;
      }

      String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
      int p = nameOfRunningVM.indexOf('@');
      return nameOfRunningVM.substring(0, p);
   }

   @Nonnull
   private String getHelpMessageForNonHotSpotVM() {
      String vmName = System.getProperty("java.vm.name");
      String helpMessage = "To run on " + vmName;

      if (vmName.contains("J9")) {
         helpMessage += ", add <IBM SDK>/lib/tools.jar to the runtime classpath (before jmockit), or";
      }

      return helpMessage + " use -javaagent:" + jarFilePath;
   }

   @Nonnull
   private VirtualMachine attachToRunningVM() {
      String pid = getProcessIdForTargetVM();

      try {
         return VirtualMachine.attach(pid);
      }
      catch (AttachNotSupportedException e) { throw new RuntimeException(e); }
      catch (IOException e) {
         if (e.getMessage().contains("current VM")) {
            //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
            throw new IllegalStateException(
               "Running on JDK 9 requires -javaagent:<proper path>/jmockit-1.n.jar or -Djdk.attach.allowAttachSelf");
         }

         throw new RuntimeException(e);
      }
   }

   private void loadAgentAndDetachFromRunningVM(@Nonnull VirtualMachine vm, @Nullable String options) {
      try {
         vm.loadAgent(jarFilePath, options);
         vm.detach();
      }
      catch (AgentLoadException | AgentInitializationException e) { throw new IllegalStateException(e); }
      catch (IOException e) { throw new RuntimeException(e); }
   }
}

/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.standalone;

import java.io.*;
import java.lang.management.*;
import java.lang.reflect.*;
import java.util.prefs.*;
import javax.management.*;

import org.jetbrains.annotations.*;

import mockit.coverage.*;

public final class CoverageControl extends StandardMBean implements CoverageControlMBean, PersistentMBean
{
   static void create()
   {
      MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

      try {
         CoverageControl mxBean = new CoverageControl();
         mbeanServer.registerMBean(mxBean, new ObjectName("JMockit Coverage:type=CoverageControl"));
      }
      catch (JMException e) {
         throw new RuntimeException(e);
      }
   }

   public CoverageControl() throws NotCompliantMBeanException, MBeanException
   {
      super(CoverageControlMBean.class);
      load();
   }

   @Override @NotNull
   protected String getDescription(MBeanInfo info)
   {
      return CoverageControlMBean.class.getAnnotation(Description.class).value();
   }

   @Override @NotNull
   protected String getParameterName(MBeanOperationInfo op, MBeanParameterInfo param, int sequence)
   {
      return "resetState";
   }

   @Override
   protected String getDescription(@NotNull MBeanAttributeInfo info) { return getDescription("get" + info.getName()); }

   @NotNull private String getDescription(@NotNull String methodName)
   {
      return getMethod(methodName).getAnnotation(Description.class).value();
   }

   @NotNull private Method getMethod(@NotNull String methodName)
   {
      for (Method method : CoverageControlMBean.class.getDeclaredMethods()) {
         if (method.getName().equals(methodName)) {
            return method;
         }
      }

      throw new IllegalStateException("Required method not found in class CoverageControlMBean: " + methodName);
   }

   @Override
   protected String getDescription(@NotNull MBeanOperationInfo info) { return getDescription(info.getName()); }

   @Override
   protected String getDescription(@NotNull MBeanOperationInfo op, MBeanParameterInfo param, int sequence)
   {
      Method method = getMethod(op.getName());
      Description desc = (Description) method.getParameterAnnotations()[sequence][0];
      return desc.value();
   }

   @Override
   protected int getImpact(MBeanOperationInfo info) { return MBeanOperationInfo.ACTION; }

   @Override @NotNull
   public String getOutput() { return getProperty("output", "html").replace("-nocp", ""); }

   @Override
   public void setOutput(@NotNull String output)
   {
      String validValues = "html serial serial-append html,serial html,serial-append serial,html serial-append,html";
      output = validateNewPropertyValue("Output", validValues, output);

      modifyConfigurationProperty("output", "html".equals(output) ? "html-nocp" : output);
   }

   @NotNull
   private String validateNewPropertyValue(
      @NotNull String propertyName, @NotNull String validValues, @NotNull String newValue)
   {
      String valueWithNoSpaces = newValue.replace(" ", "");

      if (validValues.contains(valueWithNoSpaces)) {
         return valueWithNoSpaces;
      }

      throw new IllegalArgumentException("Invalid value for \"" + propertyName + "\" property: " + newValue);
   }

   @Override @NotNull
   public String getWorkingDir() { return new File(".").getAbsoluteFile().getParent(); }

   @Override @NotNull
   public String getOutputDir() { return getProperty("outputDir"); }

   @Override
   public void setOutputDir(@NotNull String outputDir) { modifyConfigurationProperty("outputDir", outputDir); }

   @Override @NotNull
   public String getSrcDirs() { return getProperty("srcDirs"); }

   @Override
   public void setSrcDirs(@NotNull String srcDirs) { modifyConfigurationProperty("srcDirs", srcDirs); }

   @Override @NotNull
   public String getClasses() { return getProperty("classes"); }

   @Override
   public void setClasses(@NotNull String classes) { modifyConfigurationProperty("classes", classes); }

   @Override @NotNull
   public String getExcludes() { return getProperty("excludes"); }

   @Override
   public void setExcludes(@NotNull String excludes) { modifyConfigurationProperty("excludes", excludes); }

   @Override @NotNull
   public String getMetrics() { return getProperty("metrics", "all"); }

   @Override
   public void setMetrics(@NotNull String metrics)
   {
      if (metrics.isEmpty()) {
         throw new IllegalArgumentException("Please select a valid value for the \"Metrics\" property");
      }

      metrics = validateNewPropertyValue("Metrics", "all line path line,path", metrics);
      modifyConfigurationProperty("metrics", metrics);
   }

   @NotNull private String getProperty(@NotNull String property) { return getProperty(property, ""); }

   @NotNull private String getProperty(@NotNull String property, @NotNull String defaultValue)
   {
      return Configuration.getProperty(propertyNameSuffix(property), defaultValue);
   }

   @NotNull private String propertyNameSuffix(@NotNull String name)
   {
      return Character.toLowerCase(name.charAt(0)) + name.substring(1);
   }

   private void modifyConfigurationProperty(@NotNull String name, @NotNull String value)
   {
      setConfigurationProperty(name, value);
      CodeCoverage.resetConfiguration();
      store();
   }

   private void setConfigurationProperty(@NotNull String name, @NotNull String value)
   {
      Configuration.setProperty(propertyNameSuffix(name), value);
   }

   @Override
   public void generateOutput(boolean resetState)
   {
      CodeCoverage.generateOutput(resetState);
   }

   @Override
   public void load() throws MBeanException
   {
      Preferences preferences = Preferences.userNodeForPackage(CoverageControl.class);

      try {
         for (String property : preferences.keys()) {
            String commandLineValue = getProperty(property);

            if (commandLineValue.isEmpty()) {
               String value = preferences.get(property, "");
               setConfigurationProperty(property, value);
            }
         }
      }
      catch (BackingStoreException e) {
         throw new MBeanException(e);
      }
   }

   @Override
   public void store()
   {
      Preferences preferences = Preferences.userNodeForPackage(CoverageControl.class);

      for (MBeanAttributeInfo info : getMBeanInfo().getAttributes()) {
         String property = info.getName();
         String value = getProperty(property);
         preferences.put(property, value);
      }

      try {
          preferences.flush();
      }
      catch (BackingStoreException e) {
         throw new RuntimeException(e);
      }
   }
}

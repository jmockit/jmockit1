/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.standalone;

import java.io.*;
import java.lang.management.*;
import java.lang.reflect.*;
import java.util.prefs.*;
import javax.annotation.*;
import javax.management.*;

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

   @Nonnull @Override
   protected String getDescription(MBeanInfo info)
   {
      return CoverageControlMBean.class.getAnnotation(Description.class).value();
   }

   @Nonnull @Override
   protected String getParameterName(MBeanOperationInfo op, MBeanParameterInfo param, int sequence)
   {
      return "resetState";
   }

   @Override
   protected String getDescription(@Nonnull MBeanAttributeInfo info) { return getDescription("get" + info.getName()); }

   @Nonnull
   private static String getDescription(@Nonnull String methodName)
   {
      return getMethod(methodName).getAnnotation(Description.class).value();
   }

   @Nonnull
   private static Method getMethod(@Nonnull String methodName)
   {
      for (Method method : CoverageControlMBean.class.getDeclaredMethods()) {
         if (method.getName().equals(methodName)) {
            return method;
         }
      }

      throw new IllegalStateException("Required method not found in class CoverageControlMBean: " + methodName);
   }

   @Override
   protected String getDescription(@Nonnull MBeanOperationInfo info) { return getDescription(info.getName()); }

   @Override
   protected String getDescription(@Nonnull MBeanOperationInfo op, MBeanParameterInfo param, int sequence)
   {
      Method method = getMethod(op.getName());
      Description desc = (Description) method.getParameterAnnotations()[sequence][0];
      return desc.value();
   }

   @Override
   protected int getImpact(MBeanOperationInfo info) { return MBeanOperationInfo.ACTION; }

   @Nonnull @Override
   public String getOutput() { return getProperty("output", "html").replace("-nocp", ""); }

   @Override
   public void setOutput(@Nonnull String output)
   {
      String validValues = "html serial serial-append html,serial html,serial-append serial,html serial-append,html";
      output = validateNewPropertyValue("Output", validValues, output);

      modifyConfigurationProperty("output", "html".equals(output) ? "html-nocp" : output);
   }

   @Nonnull
   private static String validateNewPropertyValue(
      @Nonnull String propertyName, @Nonnull String validValues, @Nonnull String newValue)
   {
      String valueWithNoSpaces = newValue.replace(" ", "");

      if (validValues.contains(valueWithNoSpaces)) {
         return valueWithNoSpaces;
      }

      throw new IllegalArgumentException("Invalid value for \"" + propertyName + "\" property: " + newValue);
   }

   @Nonnull @Override
   public String getWorkingDir() { return new File(".").getAbsoluteFile().getParent(); }

   @Nonnull @Override
   public String getOutputDir() { return getProperty("outputDir"); }

   @Override
   public void setOutputDir(@Nonnull String outputDir) { modifyConfigurationProperty("outputDir", outputDir); }

   @Nonnull @Override
   public String getSrcDirs() { return getProperty("srcDirs"); }

   @Override
   public void setSrcDirs(@Nonnull String srcDirs) { modifyConfigurationProperty("srcDirs", srcDirs); }

   @Nonnull @Override
   public String getClasses() { return getProperty("classes"); }

   @Override
   public void setClasses(@Nonnull String classes) { modifyConfigurationProperty("classes", classes); }

   @Nonnull @Override
   public String getExcludes() { return getProperty("excludes"); }

   @Override
   public void setExcludes(@Nonnull String excludes) { modifyConfigurationProperty("excludes", excludes); }

   @Nonnull @Override
   public String getMetrics() { return getProperty("metrics", "line"); }

   @Override
   public void setMetrics(@Nonnull String metrics)
   {
      if (metrics.isEmpty()) {
         throw new IllegalArgumentException("Please select a valid value for the \"Metrics\" property");
      }

      metrics = validateNewPropertyValue("Metrics", "all line path line,path", metrics);
      modifyConfigurationProperty("metrics", metrics);
   }

   @Nonnull
   private static String getProperty(@Nonnull String property) { return getProperty(property, ""); }

   @Nonnull
   private static String getProperty(@Nonnull String property, @Nonnull String defaultValue)
   {
      return Configuration.getProperty(propertyNameSuffix(property), defaultValue);
   }

   @Nonnull
   private static String propertyNameSuffix(@Nonnull String name)
   {
      return Character.toLowerCase(name.charAt(0)) + name.substring(1);
   }

   private void modifyConfigurationProperty(@Nonnull String name, @Nonnull String value)
   {
      setConfigurationProperty(name, value);
      CodeCoverage.resetConfiguration();
      store();
   }

   private static void setConfigurationProperty(@Nonnull String name, @Nonnull String value)
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

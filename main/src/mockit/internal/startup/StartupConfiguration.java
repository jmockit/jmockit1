/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.startup;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.*;

import org.jetbrains.annotations.*;

final class StartupConfiguration
{
   @NotNull private static final Collection<String> NO_VALUES = Collections.emptyList();
   @NotNull private static final Pattern COMMA_OR_SPACES = Pattern.compile("\\s*,\\s*|\\s+");

   @NotNull private final Properties config;
   @NotNull final Collection<String> externalTools;
   @NotNull final Collection<String> mockClasses;

   StartupConfiguration() throws IOException
   {
      config = new Properties();

      loadJMockitPropertiesFilesFromClasspath();
      loadJMockitPropertiesIntoSystemProperties();

      externalTools = getMultiValuedProperty("jmockit-tools");
      mockClasses = getMultiValuedProperty("jmockit-mocks");
   }

   private void loadJMockitPropertiesFilesFromClasspath() throws IOException
   {
      Enumeration<URL> allFiles = Thread.currentThread().getContextClassLoader().getResources("jmockit.properties");
      int numFiles = 0;

      while (allFiles.hasMoreElements()) {
         URL url = allFiles.nextElement();
         InputStream propertiesFile = url.openStream();

         if (numFiles == 0) {
            try { config.load(propertiesFile); } finally { propertiesFile.close(); }
         }
         else {
            Properties properties = new Properties();
            try { properties.load(propertiesFile); } finally { propertiesFile.close(); }
            addPropertyValues(properties);
         }

         numFiles++;
      }
   }

   @SuppressWarnings("UseOfPropertiesAsHashtable")
   private void addPropertyValues(@NotNull Map<Object, Object> propertiesToAdd)
   {
      for (Entry<Object, Object> propertyToAdd : propertiesToAdd.entrySet()) {
         Object key = propertyToAdd.getKey();
         String valueToAdd = (String) propertyToAdd.getValue();
         String existingValue = (String) config.get(key);
         String newValue;

         if (existingValue == null || existingValue.isEmpty()) {
            newValue = valueToAdd;
         }
         else {
            newValue = existingValue + ' ' + valueToAdd;
         }

         config.put(key, newValue);
      }
   }

   private void loadJMockitPropertiesIntoSystemProperties()
   {
      Map<Object, Object> systemProperties = System.getProperties();

      for (Entry<Object, Object> property : config.entrySet()) {
         String key = (String) property.getKey();
         String name = key.startsWith("jmockit-") ? key : "jmockit-" + key;

         if (!systemProperties.containsKey(name)) {
            systemProperties.put(name, property.getValue());
         }
      }
   }

   @NotNull
   private static Collection<String> getMultiValuedProperty(@NotNull String key)
   {
      String commaOrSpaceSeparatedValues = System.getProperty(key);
      
      if (commaOrSpaceSeparatedValues == null) {
         return NO_VALUES;
      }

      List<String> allValues = Arrays.asList(COMMA_OR_SPACES.split(commaOrSpaceSeparatedValues));
      Set<String> uniqueValues = new HashSet<String>(allValues);
      uniqueValues.remove("");

      return uniqueValues;
   }
}

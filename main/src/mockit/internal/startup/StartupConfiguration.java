/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.startup;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.*;
import java.util.regex.Pattern;
import javax.annotation.*;

final class StartupConfiguration
{
   @Nonnull private static final Collection<String> NO_VALUES = Collections.emptyList();
   @Nonnull private static final Pattern COMMA_OR_SPACES = Pattern.compile("\\s*,\\s*|\\s+");

   @Nonnull private final Properties config;
   @Nonnull final Collection<String> externalTools;
   @Nonnull final Collection<String> mockClasses;

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
   private void addPropertyValues(@Nonnull Map<Object, Object> propertiesToAdd)
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

   @Nonnull
   private static Collection<String> getMultiValuedProperty(@Nonnull String key)
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

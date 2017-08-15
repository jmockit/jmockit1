/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection.full;

import java.beans.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import javax.annotation.*;
import javax.annotation.sql.*;
import javax.sql.*;

import mockit.internal.injection.*;

final class TestDataSource
{
   @Nullable private final String dsName;
   private Class<? extends CommonDataSource> dsClass;
   private CommonDataSource ds;

   TestDataSource(@Nonnull InjectionPoint injectionPoint) { dsName = injectionPoint.name; }

   @Nullable
   CommonDataSource createIfDataSourceDefinitionAvailable(@Nonnull TestedClass testedClass)
   {
      if (dsName == null) {
         return null;
      }

      TestedClass testedClassToBeSearched = testedClass;

      do {
         createFromTestedClassOrASuperclass(testedClassToBeSearched);

         if (ds != null) {
            return ds;
         }

         testedClassToBeSearched = testedClassToBeSearched.parent;
      }
      while (testedClassToBeSearched != null);

      throw new IllegalStateException(
         "Missing @DataSourceDefinition of name \"" + dsName + "\" on " + testedClass.nameOfTestedClass +
         " or on a super/parent class");
   }

   private void createFromTestedClassOrASuperclass(@Nonnull TestedClass testedClass)
   {
      Class<?> targetClass = testedClass.targetClass;

      do {
         createDataSource(targetClass);

         if (ds != null) {
            return;
         }

         targetClass = targetClass.getSuperclass();
      }
      while (targetClass != null && targetClass != Object.class);
   }

   private void createDataSource(@Nonnull Class<?> targetClass)
   {
      for (Annotation annotation : targetClass.getDeclaredAnnotations()) {
         String annotationName = annotation.annotationType().getName();

         if ("javax.annotation.sql.DataSourceDefinitions".equals(annotationName)) {
            createDataSource((DataSourceDefinitions) annotation);
         }
         else if ("javax.annotation.sql.DataSourceDefinition".equals(annotationName)) {
            createDataSource((DataSourceDefinition) annotation);
         }

         if (ds != null) {
            return;
         }
      }
   }

   private void createDataSource(@Nonnull DataSourceDefinitions dsDefs)
   {
      for (DataSourceDefinition dsDef : dsDefs.value()) {
         createDataSource(dsDef);

         if (ds != null) {
            return;
         }
      }
   }

   private void createDataSource(@Nonnull DataSourceDefinition dsDef)
   {
      String configuredDataSourceName = InjectionPoint.getNameFromJNDILookup(dsDef.name());

      if (configuredDataSourceName.equals(dsName)) {
         instantiateConfiguredDataSourceClass(dsDef);
         setDataSourcePropertiesFromConfiguredValues(dsDef);
      }
   }

   private void instantiateConfiguredDataSourceClass(@Nonnull DataSourceDefinition dsDef)
   {
      String className = dsDef.className();

      try {
         //noinspection unchecked
         dsClass = (Class<? extends CommonDataSource>) Class.forName(className);
         ds = dsClass.newInstance();
      }
      catch (ClassNotFoundException e) { throw new RuntimeException(e); }
      catch (InstantiationException e) { throw new RuntimeException(e.getCause()); }
      catch (IllegalAccessException e) { throw new RuntimeException(e); }
   }

   private void setDataSourcePropertiesFromConfiguredValues(@Nonnull DataSourceDefinition dsDef)
   {
      try {
         BeanInfo beanInfo = Introspector.getBeanInfo(dsClass, Object.class);
         PropertyDescriptor[] properties = beanInfo.getPropertyDescriptors();

         setProperty(properties, "url", dsDef.url());
         setProperty(properties, "user", dsDef.user());
         setProperty(properties, "password", dsDef.password());
      }
      catch (IntrospectionException e) { throw new RuntimeException(e); }
      catch (IllegalAccessException e) { throw new RuntimeException(e); }
      catch (InvocationTargetException e) { throw new RuntimeException(e); }
   }

   private void setProperty(@Nonnull PropertyDescriptor[] properties, @Nonnull String name, @Nonnull String value)
      throws InvocationTargetException, IllegalAccessException
   {
      for (PropertyDescriptor property : properties) {
         if (property.getName().equals(name)) {
            Method writeMethod = property.getWriteMethod();

            if (writeMethod != null) {
               writeMethod.invoke(ds, value);
            }

            return;
         }
      }
   }
}

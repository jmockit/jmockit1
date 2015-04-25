/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.injection;

import java.io.*;
import java.lang.annotation.*;
import javax.annotation.*;
import javax.persistence.*;
import javax.xml.parsers.*;

import static mockit.internal.expectations.injection.InjectionPoint.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * Detects and resolves dependencies belonging to the {@code javax.persistence} API, namely {@code EntityManagerFactory}
 * and {@code EntityManager}.
 */
final class JPADependencies
{
   @Nullable
   static JPADependencies createIfAvailableInClasspath(@Nonnull InjectionState injectionState)
   {
      return PERSISTENCE_UNIT_CLASS == null ? null : new JPADependencies(injectionState);
   }

   @Nullable
   static String getDependencyIdIfAvailable(@Nonnull Annotation annotation)
   {
      Class<? extends Annotation> annotationType = annotation.annotationType();

      if (annotationType == PersistenceUnit.class) {
         return ((PersistenceUnit) annotation).unitName();
      }
      else if (annotationType == PersistenceContext.class) {
         return  ((PersistenceContext) annotation).unitName();
      }

      return null;
   }

   @Nonnull private final InjectionState injectionState;
   @Nullable private String defaultPersistenceUnitName;

   private JPADependencies(@Nonnull InjectionState injectionState) { this.injectionState = injectionState; }

   @Nullable
   Object newInstanceIfApplicable(@Nonnull Class<?> dependencyType, @Nonnull Object dependencyKey)
   {
      if (dependencyType == EntityManagerFactory.class) {
         String persistenceUnitName;

         if (dependencyKey instanceof String) {
            persistenceUnitName = extractIdFromDependencyKey((String) dependencyKey);
         }
         else {
            persistenceUnitName = discoverNameOfDefaultPersistenceUnit();
         }

         EntityManagerFactory emFactory = Persistence.createEntityManagerFactory(persistenceUnitName);
         injectionState.saveInstantiatedDependency(dependencyKey, emFactory, true);
         return emFactory;
      }

      if (dependencyType == EntityManager.class) {
         return findOrCreateEntityManager(dependencyKey);
      }

      return null;
   }

   @Nonnull
   private static String extractIdFromDependencyKey(@Nonnull String dependencyKey)
   {
      int p = dependencyKey.indexOf(':');
      return dependencyKey.substring(p + 1);
   }

   @Nonnull
   private String discoverNameOfDefaultPersistenceUnit()
   {
      if (defaultPersistenceUnitName != null) {
         return defaultPersistenceUnitName;
      }

      defaultPersistenceUnitName = "<unknown>";
      InputStream xmlFile = getClass().getResourceAsStream("/META-INF/persistence.xml");

      if (xmlFile != null) {
         try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(xmlFile, new DefaultHandler() {
               @Override
               public void startElement(String uri, String localName, String qName, Attributes attributes)
               {
                  if ("persistence-unit".equals(qName)) {
                     defaultPersistenceUnitName = attributes.getValue("name");
                  }
               }
            });
            xmlFile.close();
         }
         catch (ParserConfigurationException ignore) {}
         catch (SAXException ignore) {}
         catch (IOException ignore) {}
      }

      return defaultPersistenceUnitName;
   }

   @Nullable
   private EntityManager findOrCreateEntityManager(@Nonnull Object dependencyKey)
   {
      String persistenceUnitName;
      Object emFactoryKey;

      if (dependencyKey instanceof String) {
         persistenceUnitName = extractIdFromDependencyKey((String) dependencyKey);
         emFactoryKey = EntityManagerFactory.class.getName() + ':' + persistenceUnitName;
      }
      else {
         persistenceUnitName = null;
         emFactoryKey = EntityManagerFactory.class;
      }

      EntityManagerFactory emFactory = injectionState.getInstantiatedDependency(emFactoryKey);

      if (emFactory == null) {
         if (persistenceUnitName == null) {
            persistenceUnitName = discoverNameOfDefaultPersistenceUnit();
         }

         emFactory = Persistence.createEntityManagerFactory(persistenceUnitName);
         injectionState.saveInstantiatedDependency(emFactoryKey, emFactory, true);
      }

      return emFactory.createEntityManager();
   }
}

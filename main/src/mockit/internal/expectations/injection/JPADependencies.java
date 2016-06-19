/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.injection;

import java.io.*;
import java.lang.annotation.*;
import javax.annotation.*;
import javax.persistence.*;
import javax.xml.parsers.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * Detects and resolves dependencies belonging to the {@code javax.persistence} API, namely {@code EntityManagerFactory}
 * and {@code EntityManager}.
 */
final class JPADependencies
{
   static boolean isApplicable(@Nonnull Class<?> dependencyType)
   {
      return dependencyType == EntityManager.class || dependencyType == EntityManagerFactory.class;
   }

   @Nonnull private final InjectionState injectionState;
   @Nullable private String defaultPersistenceUnitName;

   JPADependencies(@Nonnull InjectionState injectionState) { this.injectionState = injectionState; }

   @Nullable
   String getDependencyIdIfAvailable(@Nonnull Annotation annotation)
   {
      Class<? extends Annotation> annotationType = annotation.annotationType();

      if (annotationType == PersistenceUnit.class) {
         String unitName = ((PersistenceUnit) annotation).unitName();
         return getNameOfPersistentUnit(unitName);
      }
      else if (annotationType == PersistenceContext.class) {
         String unitName = ((PersistenceContext) annotation).unitName();
         return getNameOfPersistentUnit(unitName);
      }

      return null;
   }

   @Nonnull
   private String getNameOfPersistentUnit(@Nullable String injectionPointName)
   {
      return injectionPointName != null && !injectionPointName.isEmpty() ?
         injectionPointName : discoverNameOfDefaultPersistenceUnit();
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
   Object newInstanceIfApplicable(@Nonnull Class<?> dependencyType, @Nonnull InjectionPoint dependencyKey)
   {
      if (dependencyType == EntityManagerFactory.class) {
         String persistenceUnitName = getNameOfPersistentUnit(dependencyKey.name);
         InjectionPoint injectionPoint = new InjectionPoint(dependencyType, persistenceUnitName);
         EntityManagerFactory emFactory = Persistence.createEntityManagerFactory(persistenceUnitName);
         injectionState.saveGlobalDependency(injectionPoint, emFactory);
         return emFactory;
      }

      if (dependencyType == EntityManager.class) {
         return findOrCreateEntityManager(dependencyKey);
      }

      return null;
   }

   @Nullable
   private EntityManager findOrCreateEntityManager(@Nonnull InjectionPoint dependencyKey)
   {
      String persistenceUnitName = getNameOfPersistentUnit(dependencyKey.name);
      InjectionPoint emFactoryKey = new InjectionPoint(EntityManagerFactory.class, persistenceUnitName);
      EntityManagerFactory emFactory = injectionState.getGlobalDependency(emFactoryKey);

      if (emFactory == null) {
         emFactory = Persistence.createEntityManagerFactory(persistenceUnitName);
         injectionState.saveGlobalDependency(emFactoryKey, emFactory);
      }

      return emFactory.createEntityManager();
   }
}

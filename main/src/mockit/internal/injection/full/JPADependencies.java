/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection.full;

import java.io.*;
import java.lang.annotation.*;
import javax.annotation.*;
import javax.persistence.*;
import javax.xml.parsers.*;

import mockit.internal.injection.*;

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
   InjectionPoint getInjectionPointIfAvailable(@Nonnull Annotation jpaAnnotation)
   {
      Class<? extends Annotation> annotationType = jpaAnnotation.annotationType();
      Class<?> jpaClass;
      String unitName;

      if (annotationType == PersistenceUnit.class) {
         jpaClass = EntityManagerFactory.class;
         unitName = ((PersistenceUnit) jpaAnnotation).unitName();
      }
      else if (annotationType == PersistenceContext.class) {
         jpaClass = EntityManager.class;
         unitName = ((PersistenceContext) jpaAnnotation).unitName();
      }
      else {
         return null;
      }

      if (unitName.isEmpty()) {
         unitName = discoverNameOfDefaultPersistenceUnit();
      }

      return new InjectionPoint(jpaClass, unitName, true);
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

   @Nonnull
   Object createAndRegisterDependency(@Nonnull Class<?> dependencyType, @Nonnull InjectionPoint dependencyKey)
   {
      if (dependencyType == EntityManagerFactory.class) {
         InjectionPoint injectionPoint = createFactoryInjectionPoint(dependencyKey);
         EntityManagerFactory emFactory = createAndRegisterEntityManagerFactory(injectionPoint);
         return emFactory;
      }

      return createAndRegisterEntityManager(dependencyKey);
   }

   @Nonnull
   private InjectionPoint createFactoryInjectionPoint(@Nonnull InjectionPoint injectionPoint)
   {
      String persistenceUnitName = getNameOfPersistentUnit(injectionPoint.name);
      return new InjectionPoint(EntityManagerFactory.class, persistenceUnitName, injectionPoint.qualified);
   }

   @Nonnull
   private String getNameOfPersistentUnit(@Nullable String injectionPointName)
   {
      return injectionPointName != null && !injectionPointName.isEmpty() ?
         injectionPointName : discoverNameOfDefaultPersistenceUnit();
   }

   @Nonnull
   private EntityManagerFactory createAndRegisterEntityManagerFactory(@Nonnull InjectionPoint injectionPoint)
   {
      String persistenceUnitName = injectionPoint.name;
      EntityManagerFactory emFactory = Persistence.createEntityManagerFactory(persistenceUnitName);
      InjectionState.saveGlobalDependency(injectionPoint, emFactory);
      return emFactory;
   }

   @Nonnull
   private EntityManager createAndRegisterEntityManager(@Nonnull InjectionPoint injectionPoint)
   {
      InjectionPoint emFactoryKey = createFactoryInjectionPoint(injectionPoint);
      EntityManagerFactory emFactory = InjectionState.getGlobalDependency(emFactoryKey);

      if (emFactory == null) {
         emFactory = createAndRegisterEntityManagerFactory(emFactoryKey);
      }

      EntityManager entityManager = emFactory.createEntityManager();
      injectionState.saveInstantiatedDependency(injectionPoint, entityManager);
      return entityManager;
   }
}

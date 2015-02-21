/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import javax.annotation.*;
import javax.ejb.EJB;
import javax.inject.*;
import javax.persistence.*;

import org.junit.*;
import org.junit.runners.*;
import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class TestedClassWithFullStandardDITest
{
   public static class TestedClass
   {
      @Inject private Runnable dependencyToBeMocked;
      @Inject private FirstLevelDependency dependency2;
      @Resource private FirstLevelDependency dependency3;
      @Inject private CommonDependency commonDependency;
      String text;
      boolean initialized;
      static boolean destroyed;

      @PostConstruct
      void initialize()
      {
         assertNotNull(dependency3);
         initialized = true;
      }

      @PreDestroy
      void destroy()
      {
         assertTrue("TestedClass not initialized", initialized);
         destroyed = true;
      }
   }

   static final class AnotherTestedClass
   {
      @PersistenceContext EntityManager em;
   }

   public static class FirstLevelDependency
   {
      @EJB private SecondLevelDependency dependency;
      @Inject private static SecondLevelDependency staticDependency;
      @Inject private CommonDependency commonDependency;
      @Resource private static Runnable dependencyToBeMocked;
      @PersistenceContext private EntityManager em;
   }

   public static class SecondLevelDependency
   {
      @Inject CommonDependency commonDependency;
      @PersistenceContext private EntityManager em;
      boolean initialized;
      static boolean terminated;

      @PostConstruct void initialize() { initialized = true; }
      @PreDestroy void terminate() { terminated = true; }
   }

   public static class CommonDependency
   {
      @PersistenceUnit(unitName = "test") private EntityManagerFactory emFactory;
      @PersistenceContext(unitName = "test") private EntityManager em;
   }

   @Tested(fullyInitialized = true) TestedClass tested;
   @Tested(fullyInitialized = true) AnotherTestedClass tested2;
   @Injectable Runnable mockedDependency;

   static EntityManagerFactory namedEMFactory;
   static EntityManager namedEM;
   static EntityManagerFactory defaultEMFactory;
   static EntityManager defaultEM;

   @BeforeClass
   public static void setUpPersistence() throws Exception
   {
      final MockUp<EntityManager> namedEMMockUp = new MockUp<EntityManager>() {};
      final MockUp<EntityManager> defaultEMMockUp = new MockUp<EntityManager>() {};
      final MockUp<EntityManagerFactory> namedEMFactoryMockUp = new MockUp<EntityManagerFactory>() {
         @Mock
         EntityManager createEntityManager()
         {
            namedEM = namedEMMockUp.getMockInstance();
            return namedEM;
         }
      };
      final MockUp<EntityManagerFactory> defaultEMFactoryMockUp = new MockUp<EntityManagerFactory>() {
         @Mock
         EntityManager createEntityManager()
         {
            defaultEM = defaultEMMockUp.getMockInstance();
            return defaultEM;
         }
      };

      new MockUp<Persistence>() {
         @Mock
         EntityManagerFactory createEntityManagerFactory(String persistenceUnitName)
         {
            if ("test".equals(persistenceUnitName)) {
               assertNull("Named EM factory already created", namedEMFactory);
               namedEMFactory = namedEMFactoryMockUp.getMockInstance();
               return namedEMFactory;
            }
            else if ("default".equals(persistenceUnitName)) {
               assertNull("Default EM factory already created", defaultEMFactory);
               defaultEMFactory = defaultEMFactoryMockUp.getMockInstance();
               return defaultEMFactory;
            }

            fail("Unexpected persistence unit");
            return null;
         }
      };

      createTemporaryPersistenceXmlFileWithDefaultPersistenceUnit();
   }

   static void createTemporaryPersistenceXmlFileWithDefaultPersistenceUnit() throws IOException
   {
      String rootOfClasspath = TestedClass.class.getProtectionDomain().getCodeSource().getLocation().getFile();
      File tempFolder = new File(rootOfClasspath + "META-INF");
      if (tempFolder.mkdir()) tempFolder.deleteOnExit();

      File xmlFile = new File(tempFolder, "persistence.xml");
      xmlFile.deleteOnExit();

      Writer xmlWriter = new FileWriter(xmlFile);
      xmlWriter.write("<persistence><persistence-unit name='default'/></persistence>");
      xmlWriter.close();
   }

   @Test
   public void useFullyInitializedTestedObject()
   {
      // First level dependencies:
      assertSame(mockedDependency, tested.dependencyToBeMocked);
      assertNotNull(tested.dependency2);
      assertSame(tested.dependency2, tested.dependency3);
      assertNotNull(tested.commonDependency);
      assertNull(tested.text);

      // Second level dependencies:
      assertNotNull(tested.dependency2.dependency);
      assertSame(FirstLevelDependency.staticDependency, tested.dependency2.dependency);
      assertSame(tested.dependency3.dependency, tested.dependency2.dependency);
      assertSame(tested.commonDependency, tested.dependency2.commonDependency);
      assertSame(tested.commonDependency, tested.dependency3.commonDependency);
      assertSame(mockedDependency, FirstLevelDependency.dependencyToBeMocked);
      assertSame(mockedDependency, FirstLevelDependency.dependencyToBeMocked);
      assertSame(defaultEM, tested.dependency2.em);
      assertSame(tested.dependency2.em, tested.dependency3.em);
      assertSame(namedEMFactory, tested.commonDependency.emFactory);
      assertSame(namedEM, tested.commonDependency.em);
      assertNotSame(tested.dependency2.em, tested.commonDependency.em);
      assertSame(tested2.em, tested.dependency2.em);

      // Third level dependencies:
      assertSame(tested.commonDependency, tested.dependency2.dependency.commonDependency);
      assertSame(tested.dependency2.em, tested.dependency2.dependency.em);

      // Lifecycle methods:
      assertTrue(tested.initialized);
      assertTrue(tested.dependency2.dependency.initialized);
   }

   @Test
   public void useFullyInitializedTestedObjectAgain()
   {
      assertNull(tested.text);
   }

   @After
   public void verifyThatTestedFieldsWereClearedAndPreDestroyMethodsWereExecuted()
   {
      assertNull(tested);
      assertNull(tested2);
      assertTrue(TestedClass.destroyed);
      assertTrue(SecondLevelDependency.terminated);
   }
}

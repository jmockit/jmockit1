/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import javax.inject.*;
import javax.persistence.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class TestedClassWithFullStandardDITest
{
   public static class TestedClass
   {
      @Inject private Runnable dependencyToBeMocked;
      @Inject private FirstLevelDependency dependency2;
      @Inject private FirstLevelDependency dependency3;
      @Inject private CommonDependency commonDependency;
   }

   public static class FirstLevelDependency
   {
      @Inject private static SecondLevelDependency dependency;
      @Inject private CommonDependency commonDependency;
      @Inject private static Runnable dependencyToBeMocked;
      @PersistenceContext private EntityManager em;
   }

   public static class SecondLevelDependency
   {
      @Inject CommonDependency commonDependency;
      @PersistenceUnit private EntityManagerFactory emFactory;
      @PersistenceContext private EntityManager em;
   }

   public static class CommonDependency
   {
      @PersistenceUnit(unitName = "test") private EntityManagerFactory emFactory;
      @PersistenceContext(unitName = "test") private EntityManager em;
   }

   @Tested(fullyInitialized = true) TestedClass tested;
   @Injectable Runnable mockedDependency;

   @Mocked Persistence persistence;
   @Mocked EntityManagerFactory defaultEMFactory;
   @Mocked EntityManagerFactory namedEMFactory;
   @Mocked EntityManager defaultEM;
   @Mocked EntityManager namedEM;

   @Before
   public void setUpPersistence() throws Exception
   {
      new NonStrictExpectations() {{
         Persistence.createEntityManagerFactory("test"); result = namedEMFactory;
         Persistence.createEntityManagerFactory("default"); result = defaultEMFactory;
         Persistence.createEntityManagerFactory(""); result = new PersistenceException("Empty persistence unit name");

         namedEMFactory.createEntityManager(); result = namedEM;
         defaultEMFactory.createEntityManager(); result = defaultEM;
      }};

      String rootOfClasspath = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
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
      assertSame(mockedDependency, tested.dependencyToBeMocked);
      assertNotNull(tested.dependency2);
      assertSame(tested.dependency2, tested.dependency3);
      assertNotNull(tested.commonDependency);
      assertNotNull(FirstLevelDependency.dependency);
      assertSame(FirstLevelDependency.dependency, FirstLevelDependency.dependency);
      assertSame(tested.commonDependency, tested.dependency2.commonDependency);
      assertSame(tested.commonDependency, tested.dependency3.commonDependency);
      assertSame(tested.commonDependency, FirstLevelDependency.dependency.commonDependency);
      assertSame(mockedDependency, FirstLevelDependency.dependencyToBeMocked);
      assertSame(mockedDependency, FirstLevelDependency.dependencyToBeMocked);
      assertSame(defaultEM, tested.dependency2.em);
      assertSame(tested.dependency2.em, tested.dependency3.em);
      assertSame(defaultEMFactory, FirstLevelDependency.dependency.emFactory);
      assertSame(tested.dependency2.em, FirstLevelDependency.dependency.em);
      assertSame(namedEMFactory, tested.commonDependency.emFactory);
      assertSame(namedEM, tested.commonDependency.em);
      assertNotSame(tested.dependency2.em, tested.commonDependency.em);
   }
}

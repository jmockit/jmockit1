/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import java.util.*;
import javax.annotation.*;
import javax.ejb.*;
import javax.inject.*;
import javax.persistence.*;
import javax.persistence.criteria.*;
import javax.persistence.metamodel.*;
import javax.servlet.*;
import javax.servlet.http.*;

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
      @Inject HttpSession session;
      @Inject ServletContext applicationContext;
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
      @Inject ServletContext servletContext;
      @Inject HttpSession httpSession;
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

   @BeforeClass @SuppressWarnings("rawtypes")
   public static void setUpPersistence() throws Exception
   {
      final class FakeEntityManager implements EntityManager {
         @Override public void persist(Object entity) {}
         @Override public <T> T merge(T entity) { return null; }
         @Override public void remove(Object entity) {}
         @Override public <T> T find(Class<T> entityClass, Object primaryKey) { return null; }
         @Override public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties)
            { return null; }
         @Override public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) { return null; }
         @Override public <T> T find(
            Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties)
            { return null; }
         @Override public <T> T getReference(Class<T> entityClass, Object primaryKey) { return null; }
         @Override public void flush() {}
         @Override public void setFlushMode(FlushModeType flushMode) {}
         @Override public FlushModeType getFlushMode() { return null; }
         @Override public void lock(Object entity, LockModeType lockMode) {}
         @Override public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {}
         @Override public void refresh(Object entity) {}
         @Override public void refresh(Object entity, Map<String, Object> properties) {}
         @Override public void refresh(Object entity, LockModeType lockMode) {}
         @Override public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {}
         @Override public void clear() {}
         @Override public void detach(Object entity) {}
         @Override public boolean contains(Object entity) { return false; }
         @Override public LockModeType getLockMode(Object entity) { return null; }
         @Override public void setProperty(String propertyName, Object value) {}
         @Override public Map<String, Object> getProperties() { return null; }
         @Override public Query createQuery(String qlString) { return null; }
         @Override public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) { return null; }
         @Override public Query createQuery(CriteriaUpdate updateQuery) { return null; }
         @Override public Query createQuery(CriteriaDelete deleteQuery) { return null; }
         @Override public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) { return null; }
         @Override public Query createNamedQuery(String name) { return null; }
         @Override public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) { return null; }
         @Override public Query createNativeQuery(String sqlString) { return null; }
         @Override public Query createNativeQuery(String sqlString, Class resultClass) { return null; }
         @Override public Query createNativeQuery(String sqlString, String resultSetMapping) { return null; }
         @Override public StoredProcedureQuery createNamedStoredProcedureQuery(String name) { return null; }
         @Override public StoredProcedureQuery createStoredProcedureQuery(String procedureName) { return null; }
         @Override public StoredProcedureQuery createStoredProcedureQuery(
            String procedureName, Class... resultClasses) { return null; }
         @Override public StoredProcedureQuery createStoredProcedureQuery(
            String procedureName, String... resultSetMappings) { return null; }
         @Override public void joinTransaction() {}
         @Override public boolean isJoinedToTransaction() { return false; }
         @Override public <T> T unwrap(Class<T> cls) { return null; }
         @Override public Object getDelegate() { return null; }
         @Override public void close() {}
         @Override public boolean isOpen() { return false; }
         @Override public EntityTransaction getTransaction() { return null; }
         @Override public EntityManagerFactory getEntityManagerFactory() { return null; }
         @Override public CriteriaBuilder getCriteriaBuilder() { return null; }
         @Override public Metamodel getMetamodel() { return null; }
         @Override public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) { return null; }
         @Override public EntityGraph<?> createEntityGraph(String graphName) { return null; }
         @Override public EntityGraph<?> getEntityGraph(String graphName) { return null; }
         @Override public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) { return null; }
      }
      namedEM = new FakeEntityManager();
      defaultEM = new FakeEntityManager();

      final class FakeEntityManagerFactory implements EntityManagerFactory {
         final EntityManager em;
         FakeEntityManagerFactory(EntityManager em) { this.em = em; }
         @Override public EntityManager createEntityManager() { return em; }
         @Override public EntityManager createEntityManager(Map map) { return null; }
         @Override public EntityManager createEntityManager(SynchronizationType synchronizationType) { return null; }
         @Override public EntityManager createEntityManager(SynchronizationType synchronizationType, Map map)
            { return null; }
         @Override public CriteriaBuilder getCriteriaBuilder() { return null; }
         @Override public Metamodel getMetamodel() { return null; }
         @Override public boolean isOpen() { return false; }
         @Override public void close() {}
         @Override public Map<String, Object> getProperties() { return null; }
         @Override public Cache getCache() { return null; }
         @Override public PersistenceUnitUtil getPersistenceUnitUtil() { return null; }
         @Override public void addNamedQuery(String name, Query query) {}
         @Override public <T> T unwrap(Class<T> cls) { return null; }
         @Override public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {}
      }
      namedEMFactory = new FakeEntityManagerFactory(namedEM);
      defaultEMFactory = new FakeEntityManagerFactory(defaultEM);

      new MockUp<Persistence>() {
         @Mock
         EntityManagerFactory createEntityManagerFactory(String persistenceUnitName)
         {
            if ("test".equals(persistenceUnitName)) {
               return namedEMFactory;
            }

            if ("default".equals(persistenceUnitName)) {
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

   @Test
   public void verifyEmulatedHttpSession()
   {
      HttpSession session = tested2.session;
      assertFalse(session.isNew());
      assertFalse(session.getId().isEmpty());
      assertTrue(session.getCreationTime() > 0);
      assertTrue(session.getLastAccessedTime() > 0);
      assertFalse(session.getAttributeNames().hasMoreElements());

      session.setMaxInactiveInterval(600);
      assertEquals(600, session.getMaxInactiveInterval());

      session.setAttribute("test", 123);
      assertEquals(123, session.getAttribute("test"));
      assertEquals("test", session.getAttributeNames().nextElement());

      session.removeAttribute("test");
      assertNull(session.getAttribute("test"));

      session.setAttribute("test2", "abc");
      session.invalidate();

      try { session.isNew(); fail(); } catch (IllegalStateException invalidatedSession) { /* ok */ }
      try { session.getCreationTime(); fail(); } catch (IllegalStateException invalidatedSession) { /* ok */ }
      try { session.getLastAccessedTime(); fail(); } catch (IllegalStateException invalidatedSession) { /* ok */ }
      try { session.getAttributeNames(); fail(); } catch (IllegalStateException invalidatedSession) { /* ok */ }
      try { session.getAttribute("test2"); fail(); } catch (IllegalStateException invalidatedSession) { /* ok */ }
      try { session.setAttribute("x", ""); fail(); } catch (IllegalStateException invalidatedSession) { /* ok */ }
      try { session.removeAttribute("x"); fail(); } catch (IllegalStateException invalidatedSession) { /* ok */ }
      try { session.invalidate(); fail(); } catch (IllegalStateException invalidatedSession) { /* ok */ }

      assertSame(tested2.applicationContext, session.getServletContext());
      assertSame(session, tested.dependency3.dependency.httpSession);
   }

   @Test
   public void verifyEmulatedServletContext()
   {
      ServletContext ctx = tested2.applicationContext;

      assertFalse(ctx.getAttributeNames().hasMoreElements());

      ctx.setInitParameter("test", "abc");
      assertEquals("abc", ctx.getInitParameter("test"));
      assertEquals("test", ctx.getInitParameterNames().nextElement());

      ctx.setAttribute("test", 123);
      assertEquals(123, ctx.getAttribute("test"));
      assertEquals("test", ctx.getAttributeNames().nextElement());

      ctx.removeAttribute("test");
      assertNull(ctx.getAttribute("test"));

      assertSame(ctx, tested.dependency2.dependency.servletContext);
   }

   @After
   public void verifyThatTestedFieldsWereClearedAndPreDestroyMethodsWereExecuted()
   {
      assertNull(tested);
      assertNull(tested2);
      assertTrue(TestedClass.destroyed);
      assertTrue(SecondLevelDependency.terminated);
   }

   @After
   public void clearEntityManagers()
   {
      namedEM = null;
      defaultEM = null;
   }
}

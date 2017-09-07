/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import java.sql.*;
import java.util.logging.*;
import javax.annotation.*;
import javax.annotation.sql.*;
import javax.ejb.*;
import javax.enterprise.context.*;
import javax.inject.*;
import javax.sql.*;

import org.junit.*;
import static org.junit.Assert.*;

import org.springframework.beans.factory.annotation.*;

public final class TestedClassWithFullAnnotatedDITest
{
   public static class DummyDataSource implements DataSource
   {
      private String url;
      private String user;
      String password;

      public String getUrl() { return url; }
      @SuppressWarnings("unused") public void setUrl(String url) { this.url = url; }

      public String getUser() { return user; }
      @SuppressWarnings("unused") public void setUser(String user) { this.user = user; }

      @SuppressWarnings("unused") public void setPassword(String password) { this.password = password; }

      @Override public Connection getConnection() { return null; }
      @Override public Connection getConnection(String username, String password) { return null; }
      @Override public <T> T unwrap(Class<T> iface) { return null; }
      @Override public boolean isWrapperFor(Class<?> iface) { return false; }
      @Override public PrintWriter getLogWriter() { return null; }
      @Override public void setLogWriter(PrintWriter out) {}
      @Override public void setLoginTimeout(int seconds) {}
      @Override public int getLoginTimeout() { return 0; }
      @SuppressWarnings("Since15") @Override public Logger getParentLogger() { return null; }
   }

   @DataSourceDefinition(
      name = "java:global/jdbc/testDS", className = "mockit.TestedClassWithFullAnnotatedDITest$DummyDataSource",
      url = "jdbc:testDb:test", user = "tests", password = "test123")
   static final class TestedClass
   {
      @Inject Runnable action;
      @Autowired ItfWithSingleImpl dependency1;
      @Resource ItfWithSingleImpl dependency2;
      @Inject ItfWithTwoImpls anotherDependency;
      @Inject private Logger log1;
      @Inject private Logger log2;
      Collaborator collaborator;
      @Inject Conversation conversation;
      @Resource(lookup = "java:global/jdbc/testDS") DataSource ds;
   }

   public static final class PooledDataSource extends DummyDataSource implements ConnectionPoolDataSource {
      @Override public PooledConnection getPooledConnection() { return null; }
      @Override public PooledConnection getPooledConnection(String user, String password) { return null; }
   }

   public static final class DistributedDataSource extends DummyDataSource implements XADataSource {
      @Override public XAConnection getXAConnection() { return null; }
      @Override public XAConnection getXAConnection(String user, String password) { return null; }
   }

   @DataSourceDefinitions({
      @DataSourceDefinition(
         name = "regularDS", className = "mockit.TestedClassWithFullAnnotatedDITest$DummyDataSource",
         url = "jdbc:oracle:test", user = "tests", password = "test123"),
      @DataSourceDefinition(
         name = "pooledDS", className = "mockit.TestedClassWithFullAnnotatedDITest$PooledDataSource",
         url = "jdbc:hsqldb:db", user = "pool", password = "test123"),
      @DataSourceDefinition(
         name = "distributedDS", className = "mockit.TestedClassWithFullAnnotatedDITest$DistributedDataSource",
         url = "jdbc:postgresql:database", user = "xa", password = "test123")
   })
   static class AnotherTestedClass
   {
      @Resource(lookup = "regularDS") DataSource ds1;
      @Resource(lookup = "pooledDS") ConnectionPoolDataSource ds2;
      @Resource(lookup = "distributedDS") XADataSource ds3;
      @Resource(name = "regularDS") DataSource ds4;
   }

   static class Collaborator {}

   public interface ItfWithSingleImpl {}
   public static final class SingleImpl implements ItfWithSingleImpl { @EJB ItfToBeMocked ejb; }

   public interface ItfWithTwoImpls {}
   @SuppressWarnings("unused") public static final class Impl1 implements ItfWithTwoImpls {}
   public static final class Impl2 implements ItfWithTwoImpls {}

   public interface ItfToBeMocked {}

   @Tested SingleImpl dep1;
   @Tested Impl2 anotherDep;
   @Tested Collaborator collaborator;
   @Tested(fullyInitialized = true) TestedClass tested;
   // Without these injectables, a "missing @Injectable" exception occurs for each unresolved field.
   @Injectable Runnable action;
   @Injectable ItfToBeMocked ejb;

   @Test
   public void injectInitializedDependenciesForInterfacesHavingTestedObjectsOfImplementationClassTypes()
   {
      assertSame(action, tested.action);
      assertNotNull(tested.dependency1);
      assertSame(tested.dependency1, tested.dependency2);
      assertTrue(tested.anotherDependency instanceof Impl2);
      assertSame(ejb, ((SingleImpl) tested.dependency1).ejb);
   }

   @Test
   public void injectLoggerFieldsWithLoggerCreatedWithTestedClassName()
   {
      assertEquals(TestedClass.class.getName(), tested.log1.getName());
      assertSame(tested.log2, tested.log1);
   }

   @Test
   public void injectNonAnnotatedFieldFromMatchingTestedField()
   {
      assertSame(collaborator, tested.collaborator);
   }

   @Tested Conversation conversation;

   @Test
   public void manageConversationContext()
   {
      assertNotNull(conversation);
      assertSame(tested.conversation, conversation);
      assertTrue(conversation.isTransient());

      assertEquals(0, conversation.getTimeout());
      conversation.setTimeout(1500);
      assertEquals(1500, conversation.getTimeout());

      assertNull(conversation.getId());

      conversation.begin();
      assertFalse(conversation.isTransient());
      assertNotNull(conversation.getId());

      conversation.end();
      assertTrue(conversation.isTransient());
      assertNull(conversation.getId());

      conversation.begin("test");
      assertFalse(conversation.isTransient());
      assertEquals("test", conversation.getId());
   }

   @Test
   public void injectDataSourceConfiguredFromSingleDataSourceDefinition()
   {
      assertTrue(tested.ds instanceof DummyDataSource);

      DummyDataSource ds = (DummyDataSource) tested.ds;
      assertEquals("jdbc:testDb:test", ds.getUrl());
      assertEquals("tests", ds.getUser());
      assertEquals("test123", ds.password);
   }

   @Tested(fullyInitialized = true) AnotherTestedClass tested2;

   @Test
   public void injectMultipleDataSourcesConfiguredFromDifferentDataSourceDefinitions()
   {
      assertTrue(tested2.ds1 instanceof DummyDataSource);
      assertTrue(tested2.ds2 instanceof PooledDataSource);
      assertTrue(tested2.ds3 instanceof DistributedDataSource);
      assertSame(tested2.ds1, tested2.ds4);
   }
}

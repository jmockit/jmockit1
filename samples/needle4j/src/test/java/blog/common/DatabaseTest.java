package blog.common;

import java.util.*;
import javax.annotation.sql.*;
import javax.persistence.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

import blog.user.*;

@DataSourceDefinition(name = "ds", className = "org.hsqldb.jdbc.JDBCDataSource", url = "jdbc:hsqldb:.", user = "sa")
public final class DatabaseTest
{
   @ObjectUnderTest Database db;
   @Tested EntityManager em; // extract Database's em

   @Test
   public void findWithSQL() {
      em.getTransaction().begin();
      User user = new User();
      user.setFirstName("John");
      user.setSurname("Tester");
      user.setUsername("tester");
      user.setPassword("12345");
      db.save(user);
      em.getTransaction().commit();

      // Uses the same JDBC connection as currently assigned the EntityManager.
      List<Object[]> users = db.findWithSQL("select * from User");

      assertFalse(users.isEmpty());
      Object[] aUser = users.get(0);
      assertEquals(6, aUser.length);
   }
}
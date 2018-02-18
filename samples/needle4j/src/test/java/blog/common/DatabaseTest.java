package blog.common;

import java.util.*;
import javax.annotation.sql.*;

import org.junit.*;
import static org.junit.Assert.*;

@DataSourceDefinition(name = "ds", className = "org.hsqldb.jdbc.JDBCDataSource", url = "jdbc:hsqldb:.", user = "sa")
public final class DatabaseTest
{
   @ObjectUnderTest Database db;

   @Test
   public void findWithSQL() {
      List<Object[]> users = db.findWithSQL("select * from User");

      assertTrue(users.isEmpty());
   }
}
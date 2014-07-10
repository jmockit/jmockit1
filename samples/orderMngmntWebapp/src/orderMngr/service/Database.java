/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package orderMngr.service;

import java.sql.*;

/**
 * A static facade for database access through JDBC.
 * It assumes the application can use a single global DB connection.
 * <p/>
 * This class is just for the sake of demonstrating the capabilities of the available mocking APIs.
 * In the real world, such direct use of JDBC is not a practical approach, leading to long and complicated methods,
 * with excessive amounts of mocking in the corresponding unit tests.
 * A better approach is to use a higher-level abstraction for access to persistent state, which allows both production
 * and test code to be significantly smaller.
 */
public final class Database
{
   private static Connection connection;

   private Database() {}

   public static synchronized Connection connection()
   {
      if (connection == null) {
         try {
            connection = DriverManager.getConnection("jdbc:test:ordersDB");
         }
         catch (SQLException e) {
            throw new RuntimeException(e);
         }
      }

      return connection;
   }

   public static void executeInsertUpdateOrDelete(String sql, Object... args)
   {
      try (PreparedStatement stmt = createStatement(sql, args)) {
         stmt.executeUpdate();
      }
      catch (SQLException e) {
         throw new RuntimeException(e);
      }
   }

   private static PreparedStatement createStatement(String sql, Object... args) throws SQLException
   {
      PreparedStatement stmt = connection().prepareStatement(sql);
      int i = 1;

      for (Object arg : args) {
         stmt.setObject(i, arg);
         i++;
      }

      return stmt;
   }

   public static void closeStatement(ResultSet result)
   {
      if (result != null) {
         try {
            result.getStatement().close();
         }
         catch (SQLException e) {
            throw new RuntimeException(e);
         }
      }
   }

   public static ResultSet executeQuery(String sql, Object... args)
   {
      try {
         PreparedStatement stmt = createStatement(sql, args);
         return stmt.executeQuery();
      }
      catch (SQLException e) {
         throw new RuntimeException(e);
      }
   }
}

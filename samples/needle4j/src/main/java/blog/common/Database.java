package blog.common;

import java.sql.*;
import java.util.*;
import javax.annotation.*;
import javax.ejb.*;
import javax.persistence.*;
import javax.sql.*;

@Stateless
public class Database
{
   @Resource private DataSource ds;
   @PersistenceContext private EntityManager em;

   public Long save(BaseEntity instance) {
      if (instance.getId() == null) {
         em.persist(instance);
      }
      else {
         instance = em.merge(instance);
         em.flush();
      }

      return instance.getId();
   }

   public void remove(BaseEntity instance) {
      if (em.contains(instance)) {
         em.remove(instance);
      }
      else {
         BaseEntity persistentInstance = em.find(instance.getClass(), instance.getId());

         if (persistentInstance != null) {
            em.remove(persistentInstance);
         }
      }
   }

   public <E extends BaseEntity> E find(Class<E> entityClass, long id) {
      return em.find(entityClass, id);
   }

   public <S> S findSingle(String ql, Object... args) {
      Query query = createQuery(ql, args);

      try {
         @SuppressWarnings("unchecked") S result = (S) query.getSingleResult();
         return result;
      }
      catch (NoResultException ignore) {
         return null;
      }
   }

   private Query createQuery(String ql, Object... args) {
      Query query = em.createQuery(ql);

      for (int i = 0; i < args.length; i++) {
         Object arg = args[i];
         query.setParameter(1 + i, arg);
      }

      return query;
   }

   public <E extends BaseEntity> List<E> find(String ql, Object... args) {
      Query query = createQuery(ql, args);

      @SuppressWarnings("unchecked") List<E> resultList = query.getResultList();
      return resultList;
   }

   public <E extends BaseEntity> List<E> findWithPaging(String ql, int maxResults, int firstResult, Object... args) {
      Query query = createQuery(ql, args);
      query.setMaxResults(maxResults).setFirstResult(firstResult);

      @SuppressWarnings("unchecked") List<E> resultList = query.getResultList();
      return resultList;
   }

   public List<Object[]> findWithSQL(String sql, Object... args) {
      try (Connection con = ds.getConnection()) {
         return executeSQLQuery(con, sql, args);
      }
      catch (SQLException e) {
         throw new RuntimeException(e);
      }
   }

   private static List<Object[]> executeSQLQuery(Connection con, String sql, Object... args) throws SQLException {
      try (PreparedStatement stmt = con.prepareStatement(sql)) {
         for (int i = 0; i < args.length; i++) {
            stmt.setObject(i + 1, args[i]);
         }

         return executeSQLQuery(stmt);
      }
   }

   private static List<Object[]> executeSQLQuery(PreparedStatement stmt) throws SQLException {
      try (ResultSet rs = stmt.executeQuery()) {
         int columns = rs.getMetaData().getColumnCount();
         List<Object[]> result = new ArrayList<>();

         while (rs.next()) {
            Object[] values = new Object[columns];

            for (int i = 0; i < columns; i++) {
               values[i] = rs.getObject(i + 1);
            }

            result.add(values);
         }

         return result;
      }
   }
}

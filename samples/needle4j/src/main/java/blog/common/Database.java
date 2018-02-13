package blog.common;

import java.util.*;
import javax.ejb.*;
import javax.persistence.*;

@Stateless
public class Database
{
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
}

package petclinic.util;

import java.util.*;
import javax.persistence.*;
import javax.transaction.*;

@Transactional
public class Database
{
   @PersistenceContext private EntityManager em;

   public <E extends BaseEntity> E findById(Class<E> entityClass, int id)
   {
      E entity = em.find(entityClass, id);
      return entity;
   }

   public <E extends BaseEntity> List<E> find(String qlStatement, Object... qlArgs)
   {
      return find(0, qlStatement, qlArgs);
   }

   @SuppressWarnings("unchecked")
   public <E extends BaseEntity> List<E> find(int maxResults, String qlStatement, Object... qlArgs)
   {
      Query query = em.createQuery(qlStatement);
      query.setMaxResults(maxResults);

      for (int i = 0; i < qlArgs.length; i++) {
         query.setParameter(i + 1, qlArgs[i]);
      }

      List<E> resultList = query.getResultList();
      return resultList;
   }

   public void save(BaseEntity entity)
   {
      if (entity.getId() == null) {
         em.persist(entity);
      }
      else {
         if (!em.contains(entity)) { // in case it is a detached entity
            em.merge(entity);
         }

         em.flush();
      }
   }
}

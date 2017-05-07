package petclinic.util;

import java.util.*;
import javax.annotation.*;
import javax.persistence.*;
import javax.transaction.*;

/**
 * Provides access to the application database, allowing transient instances of entity classes to be persisted, and
 * persistent instances to be recovered or removed from the database.
 */
@Transactional
public class Database
{
   @PersistenceContext private EntityManager em;

   /**
    * Finds an entity in the application database given its class and unique id.

    * @return the persistent entity if found, or {@code null} if not found
    */
   @Nullable
   public <E extends BaseEntity> E findById(@Nonnull Class<E> entityClass, int id)
   {
      E entity = em.find(entityClass, id);
      return entity;
   }

   /**
    * Finds one or more persistent entities of a certain type in the application database.
    *
    * @param qlStatement a JPQL "select" statement that locates entities of the same type
    * @param qlArgs zero or more argument values for the positional query parameters specified in the JPQL statement,
    *               in the same order as the parameter positions
    *
    * @return the list of zero or more entities found, in an arbitrary order or in the order specified by an "order by"
    * clause (if any)
    *
    * @see #find(int, String, Object...)
    */
   @Nonnull
   public <E extends BaseEntity> List<E> find(@Nonnull String qlStatement, @Nonnull Object... qlArgs)
   {
      return find(0, qlStatement, qlArgs);
   }

   /**
    * Finds one or more persistent entities of a certain type in the application database, up to a given maximum number
    * of entities.
    *
    * @param maxResults the maximum number of resulting entities to be returned, or {@code 0} if there is no limit
    * @param qlStatement a JPQL "select" statement that locates entities of the same type
    * @param qlArgs zero or more argument values for the positional query parameters specified in the JPQL statement,
    *               in the same order as the parameter positions
    *
    * @return the list of zero or more entities found, in an arbitrary order or in the order specified by an "order by"
    * clause (if any)
    */
   @Nonnull
   public <E extends BaseEntity> List<E> find(
      @Nonnegative int maxResults, @Nonnull String qlStatement, @Nonnull Object... qlArgs)
   {
      Query query = em.createQuery(qlStatement);
      query.setMaxResults(maxResults);

      for (int i = 0; i < qlArgs.length; i++) {
         query.setParameter(i + 1, qlArgs[i]);
      }

      @SuppressWarnings("unchecked") List<E> resultList = query.getResultList();
      return resultList;
   }

   /**
    * Saves the state of a given entity to the application database, whether it is new (still transient, with no id) or
    * already persisted (with an id).
    * <p/>
    * In the case of an already persisted entity, the persistence context is synchronized to the application database,
    * so that any pending "inserts", "updates" or "deletes" get executed at this time.
    */
   public void save(@Nonnull BaseEntity entity)
   {
      if (entity.isNew()) {
         em.persist(entity);
      }
      else {
         if (!em.contains(entity)) { // in case it is a detached entity
            em.merge(entity);
         }

         em.flush();
      }
   }

   /**
    * Removes a given persistent entity from the application database.
    */
   public void remove(@Nonnull BaseEntity entity)
   {
      em.remove(entity);
   }
}

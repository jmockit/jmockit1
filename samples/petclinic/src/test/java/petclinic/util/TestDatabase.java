package petclinic.util;

import java.util.*;
import javax.annotation.*;
import javax.inject.*;
import javax.persistence.*;

import static org.junit.Assert.*;

/**
 * Ensures that tests execute in a database transaction which is always rolled back at the end.
 */
public class TestDatabase
{
   @PersistenceContext private EntityManager em;
   @Inject protected Database db;

   @PostConstruct
   private void beginTransactionIfNotYet()
   {
      EntityTransaction transaction = em.getTransaction();

      if (!transaction.isActive()) {
         transaction.begin();
      }
   }

   @PreDestroy
   private void endTransactionWithRollbackIfStillActive()
   {
      EntityTransaction transaction = em.getTransaction();

      if (transaction.isActive()) {
         transaction.rollback();
      }
   }

   public final void refresh(BaseEntity entity)
   {
      em.flush();
      em.refresh(entity);
   }

   public final <E extends BaseEntity> E findOne(Class<E> entityClass)
   {
      return findOne("select e from " + entityClass.getSimpleName() + " e");
   }

   @SuppressWarnings("unchecked")
   public final <E extends BaseEntity> E findOne(String qlStatement, Object... qlArgs)
   {
      List<BaseEntity> entities = db.find(1, qlStatement, qlArgs);
      return entities.isEmpty() ? null : (E) entities.get(0);
   }

   public final void assertCreated(BaseEntity newEntity, String qlStatement, Object... qlArgs)
   {
      Integer newId = newEntity.getId();
      assertNotNull("id is still null", newId);
      assertTrue(newId >= 0);

      List<? extends BaseEntity> found = db.find(qlStatement, qlArgs);

      for (BaseEntity entityFound : found) {
         if (entityFound.getId().equals(newId)) {
            return;
         }
      }

      fail("New entity with id " + newId + " not found in database");
   }
}

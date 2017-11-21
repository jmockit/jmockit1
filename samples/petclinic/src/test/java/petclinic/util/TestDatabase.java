package petclinic.util;

import java.util.*;
import javax.annotation.*;
import javax.inject.*;
import javax.persistence.*;

import static org.junit.Assert.*;

/**
 * Base test utility class for domain-specific "EntityXData" classes, which allow each test to create
 * the required data in the test database.
 * <p/>
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

   /**
    * Refreshes the persistent state of a given entity from the database, so that a test can verify
    * that persistent state was modified as expected.
    */
   public final void refresh(BaseEntity entity)
   {
      em.refresh(entity);
   }

   /**
    * Finds one entity of the desired type.
    *
    * @param qlStatement a JPQL statement for finding a list of entities of which the first gets returned
    * @param qlArgs zero or more arguments for positional parameters in the JPQL statement
    * @param <E> specifies the desired entity type
    *
    * @return the first entity found, if any, or <tt>null</tt> if none
    */
   @SuppressWarnings("unchecked")
   public final <E extends BaseEntity> E findOne(String qlStatement, Object... qlArgs)
   {
      List<BaseEntity> entities = db.find(1, qlStatement, qlArgs);
      return entities.isEmpty() ? null : (E) entities.get(0);
   }

   /**
    * Verifies that a given entity was properly inserted into the database, including the generation of its integer
    * identifier.
    *
    * @param newEntity an entity just persisted
    * @param qlStatement a JPQL statement for finding a set of entities which must contain the new entity
    * @param qlArgs zero or more arguments for positional parameters in the JPQL statement
    *
    * @throws AssertionError if the new entity has a null or negative id, or if it is not among the entities found
    * by executing the given JPQL statement
    */
   public final void assertCreated(BaseEntity newEntity, String qlStatement, Object... qlArgs)
   {
      Integer newId = newEntity.getId();
      assertNotNull("id is still null", newId);
      assertTrue(newId >= 0);

      List<? extends BaseEntity> found = db.find(qlStatement, qlArgs);

      if (found.stream().filter(e -> e.getId().equals(newId)).count() > 0) {
         return;
      }

      fail("New entity with id " + newId + " not found in database");
   }
}

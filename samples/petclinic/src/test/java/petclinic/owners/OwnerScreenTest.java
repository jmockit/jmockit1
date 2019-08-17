package petclinic.owners;

import java.util.*;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import petclinic.util.*;

/**
 * Integration tests for {@link Owner}-related operations, at the application service level.
 * Each test runs in a database transaction that is rolled back at the end of the test.
 */
final class OwnerScreenTest
{
   @TestUtil OwnerData ownerData;
   @SUT OwnerScreen ownerScreen;

   @Test
   void findOwnersByFullLastName() {
      Owner davis = ownerData.create("Tom Davis");

      ownerScreen.setLastName(davis.getLastName());
      ownerScreen.findOwners();
      List<Owner> ownersWithTheGivenLastName = ownerScreen.getOwners();

      assertTrue(ownersWithTheGivenLastName.contains(davis));
      assertEquals(davis.getLastName(), ownerScreen.getLastName());
   }

   @Test
   void findOwnersByLastNamePrefix() {
      Owner esteban = ownerData.create("Jaime Esteban");

      ownerScreen.setLastName("Es");
      ownerScreen.findOwners();
      List<Owner> ownersWithLastNameHavingGivenPrefix = ownerScreen.getOwners();

      assertTrue(ownersWithLastNameHavingGivenPrefix.contains(esteban));
   }

   @Test
   void findOwnersByLastNameWithNoSuchOwners() {
      ownerScreen.setLastName("Daviss");
      ownerScreen.findOwners();
      List<Owner> ownersWithNonExistingLastName = ownerScreen.getOwners();

      assertTrue(ownersWithNonExistingLastName.isEmpty());
   }

   @Test
   void findOwnersWithAnyLastName() {
      Owner davis = ownerData.create("Tom Davis");
      Owner esteban = ownerData.create("Jaime Esteban");

      ownerScreen.findOwners();
      List<Owner> allOwners = ownerScreen.getOwners();

      assertTrue(allOwners.contains(davis));
      assertTrue(allOwners.contains(esteban));
   }

   @Test
   void createNewOwner() {
      ownerScreen.requestNewOwner();
      Owner owner = ownerScreen.getOwner();
      owner.setFirstName("Sam");
      owner.setLastName("Schultz");
      owner.setAddress("4, Evans Street");
      owner.setCity("Wollongong");
      owner.setTelephone("4444444444");

      ownerScreen.createOrUpdateOwner();

      ownerData.assertCreated(owner, "select o from Owner o where o.firstName = 'Sam' and o.lastName = 'Schultz'");
   }

   @Test
   void updateExistingOwner() {
      Owner owner = ownerData.create("An owner");
      ownerScreen.selectOwner(owner.getId());

      String newLastName = owner.getLastName() + "X";
      owner.setLastName(newLastName);
      ownerScreen.createOrUpdateOwner();

      Owner modifiedOwner =
         ownerData.findOne("select o from Owner o where o = ?1 and o.lastName = ?2", owner, newLastName);
      assertNotNull(modifiedOwner);
      assertEquals(owner.getFirstName(), modifiedOwner.getFirstName());
      assertEquals(owner.getAddress(), modifiedOwner.getAddress());
      assertEquals(owner.getCity(), modifiedOwner.getCity());
      assertEquals(owner.getTelephone(), modifiedOwner.getTelephone());
   }
}
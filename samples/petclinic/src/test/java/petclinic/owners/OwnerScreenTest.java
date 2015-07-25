package petclinic.owners;

import java.util.*;

import org.junit.*;
import static org.junit.Assert.*;

import petclinic.util.*;

public final class OwnerScreenTest
{
   @TestUtil OwnerData ownerData;
   @SUT OwnerScreen ownerScreen;

   @Test
   public void findOwnersByFullLastName()
   {
      Owner davis = ownerData.create("Tom Davis");

      ownerScreen.setLastName("Davis");
      ownerScreen.findOwners();
      List<Owner> ownersWithTheGivenLastName = ownerScreen.getOwners();

      assertTrue(ownersWithTheGivenLastName.contains(davis));
   }

   @Test
   public void findOwnersByLastNamePrefix()
   {
      Owner esteban = ownerData.create("Jaime Esteban");

      ownerScreen.setLastName("Es");
      ownerScreen.findOwners();
      List<Owner> ownersWithLastNameHavingGivenPrefix = ownerScreen.getOwners();

      assertTrue(ownersWithLastNameHavingGivenPrefix.contains(esteban));
   }

   @Test
   public void findOwnersByLastNameWithNoSuchOwners()
   {
      ownerScreen.setLastName("Daviss");
      ownerScreen.findOwners();
      List<Owner> ownersWithNonExistingLastName = ownerScreen.getOwners();

      assertTrue(ownersWithNonExistingLastName.isEmpty());
   }

   @Test
   public void createNewOwner()
   {
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
   public void updateExistingOwner()
   {
      Owner owner = ownerData.create("An owner");
      ownerScreen.selectOwner(owner.getId());

      String newLastName = owner.getLastName() + "X";
      owner.setLastName(newLastName);
      ownerScreen.createOrUpdateOwner();

      Owner modifiedOwner =
         ownerData.findOne("select o from Owner o where o = ?1 and o.lastName = ?2", owner, newLastName);
      assertNotNull(modifiedOwner);
   }
}

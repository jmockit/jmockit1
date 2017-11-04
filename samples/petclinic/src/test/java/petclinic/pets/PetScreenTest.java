package petclinic.pets;

import java.util.*;
import javax.validation.ValidationException;
import static java.util.Arrays.*;

import org.junit.*;
import org.junit.rules.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import petclinic.owners.*;
import petclinic.util.*;

/**
 * Integration tests for {@link Pet}-related operations, at the application service level.
 * Each test runs in a database transaction that is rolled back at the end of the test.
 */
public final class PetScreenTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();
   @TestUtil OwnerData ownerData;
   @TestUtil PetData petData;
   @SUT PetScreen petScreen;

   @Test
   public void findAllPetTypes()
   {
      PetType type1 = petData.createType("type1");
      PetType type2 = petData.createType("Another type");

      List<PetType> petTypes = petScreen.getTypes();
      List<PetType> petTypesAgain = petScreen.getTypes();

      petTypes.retainAll(asList(type1, type2));
      assertSame(type1, petTypes.get(1));
      assertSame(type2, petTypes.get(0));
      assertSame(petTypes, petTypesAgain);
   }

   @Test
   public void createPetWithGeneratedId()
   {
      String petName = "bowser";
      Owner owner = ownerData.create("The Owner");
      assumeTrue(owner.getPet(petName) == null);
      petScreen.selectOwner(owner.getId());

      PetType type = petData.findOrCreatePetType("dog");
      assertEquals("dog", type.getName());

      petScreen.requestNewPet();
      Pet pet = petScreen.getPet();
      pet.setName(petName);
      pet.setType(type);
      pet.setBirthDate(new Date());
      petScreen.createOrUpdatePet();

      assertNotNull(pet.getId());
      assertSame(owner, pet.getOwner());
      assertEquals(1, owner.getPets().size());
      assertSame(pet, owner.getPet(petName));
   }

   @Test
   public void attemptToCreatePetWithDuplicateNameForSameOwner()
   {
      Owner owner = ownerData.create("The Owner");
      petScreen.selectOwner(owner.getId());
      Date birthDate = new GregorianCalendar(2005, Calendar.AUGUST, 6).getTime();
      Pet ownedPet = petData.create(owner, "Buck", birthDate, "dog");

      petScreen.requestNewPet();
      Pet secondPet = petScreen.getPet();
      secondPet.setName(ownedPet.getName());

      thrown.expect(ValidationException.class);
      thrown.expectMessage("owner already has a pet with this name");

      petScreen.createOrUpdatePet();
   }

   @Test
   public void attemptToCreatePetWithoutAnOwnerHavingBeenSelected()
   {
      petScreen.createOrUpdatePet();

      assertNull(petScreen.getPet());
   }

   @Test
   public void updatePetName()
   {
      Date birthDate = new GregorianCalendar(2005, Calendar.AUGUST, 6).getTime();
      Pet pet = petData.create("Pet", birthDate, "cat");
      petScreen.selectPet(pet.getId());

      String oldName = pet.getName();
      String newName = oldName + "X";
      pet.setName(newName);
      petScreen.createOrUpdatePet();

      Pet petUpdated = petScreen.getPet();
      petData.refresh(petUpdated);
      assertEquals(newName, petUpdated.getName());
      assertEquals(pet.getBirthDate(), petUpdated.getBirthDate());
      assertEquals(pet.getType(), petUpdated.getType());
   }
}

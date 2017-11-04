package petclinic.visits;

import java.util.*;
import static java.util.Arrays.*;

import org.junit.*;
import static org.junit.Assert.*;

import petclinic.pets.*;
import petclinic.util.*;

/**
 * Integration tests for {@link Visit}-related operations, at the application service level.
 * Each test runs in a database transaction that is rolled back at the end of the test.
 */
public final class VisitScreenTest
{
   @TestUtil PetData petData;
   @TestUtil VisitData visitData;
   @SUT VisitScreen visitScreen;

   @Test
   public void showVisitsForSelectedPet()
   {
      Visit v1 = visitData.create("Visit 1 for pet");
      Visit v2 = visitData.create("Visit 2 for pet");
      visitScreen.selectPet(v1.getPet().getId());

      visitScreen.showVisits();
      List<Visit> visits = visitScreen.getVisits();

      assertEquals(asList(v1, v2), visits);
   }

   @Test
   public void attemptToShowVisitsWithoutFirstSelectingAPet()
   {
      visitScreen.showVisits();

      assertNull(visitScreen.getVisits());
   }

   @Test
   public void attemptToSelectVisitWithNonExistingId()
   {
      Visit visit = visitData.create("Visit 1 for pet");
      visitScreen.selectVisit(visit.getId());

      visitScreen.selectVisit(95234232);

      assertNull(visitScreen.getVisit());
      assertNull(visitScreen.getPet());
   }

   @Test
   public void addNewVisitForPet()
   {
      Pet pet = petData.create("Samantha", null, "hamster");

      visitScreen.requestNewVisit();
      Visit visit = visitScreen.getVisit();
      visit.setDescription("test");

      visitScreen.selectPet(pet.getId());
      assertSame(pet, visitScreen.getPet());

      visitScreen.createOrUpdateVisit();

      petData.refresh(pet);
      assertSame(pet, visit.getPet());
      assertEquals(1, pet.getVisits().size());
      assertNotNull(visit.getId());
      assertNotNull(visit.getDate());
   }

   @Test
   public void updateExistingVisit()
   {
      Visit visit = visitData.create();
      visitScreen.selectVisit(visit.getId());

      Visit editedVisited = visitScreen.getVisit();
      String modifiedDescription = editedVisited.getDescription() + " - modified";
      editedVisited.setDescription(modifiedDescription);

      visitScreen.createOrUpdateVisit();

      Visit modifiedVisit = visitScreen.getVisit();
      visitData.refresh(modifiedVisit);
      assertEquals(modifiedDescription, modifiedVisit.getDescription());
   }

   @Test
   public void attemptToCreateOrUpdateVisitWithoutFirstSelectingAPet()
   {
      visitScreen.createOrUpdateVisit();

      assertNull(visitScreen.getVisit());
   }

   @Test
   public void attemptToUpdateVisitWithoutFirstSelectingAVisit()
   {
      Pet pet = petData.create("Samantha", null, "hamster");
      visitScreen.selectPet(pet.getId());

      visitScreen.createOrUpdateVisit();

      assertNull(visitScreen.getVisit());
   }
}

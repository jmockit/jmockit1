package petclinic.visits;

import java.util.*;
import static java.util.Arrays.*;

import org.junit.*;
import static org.junit.Assert.*;

import petclinic.pets.*;
import petclinic.util.*;

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
}

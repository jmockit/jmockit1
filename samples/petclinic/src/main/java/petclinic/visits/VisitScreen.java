package petclinic.visits;

import java.util.*;
import javax.annotation.*;
import javax.faces.view.*;
import javax.inject.*;
import javax.transaction.*;

import petclinic.pets.*;

/**
 * An application service class that handles {@link Visit}-related operations from the visit screen.
 */
@Named @Transactional @ViewScoped
public class VisitScreen
{
   @Inject private VisitMaintenance visitMaintenance;
   @Inject private PetMaintenance petMaintenance;
   @Nullable private Pet pet;
   @Nullable private Visit visit;
   @Nullable private List<Visit> visits;

   @Nullable public Pet getPet() { return pet; }
   @Nullable public Visit getVisit() { return visit; }
   @Nullable public List<Visit> getVisits() { return visits; }

   public void selectPet(int petId)
   {
      pet = petMaintenance.findById(petId);
   }

   public void selectVisit(int visitId)
   {
      visit = visitMaintenance.findById(visitId);
      assert visit != null;
      pet = visit.getPet();
   }

   public void requestNewVisit()
   {
      visit = new Visit();
   }

   public void createOrUpdateVisit()
   {
      assert pet != null : "Pet not selected";
      assert visit != null : "Visit not selected";
      visitMaintenance.create(pet, visit);
   }

   public void showVisits()
   {
      assert pet != null : "Pet not selected";
      visits = visitMaintenance.findByPetId(pet.getId());
   }
}

package petclinic.visits;

import java.util.*;
import javax.faces.view.*;
import javax.inject.*;
import javax.transaction.*;

import petclinic.pets.*;

@Named @Transactional @ViewScoped
public class VisitScreen
{
   @Inject private VisitMaintenance visitMaintenance;
   @Inject private PetMaintenance petMaintenance;
   private Pet pet;
   private Visit visit;
   private List<Visit> visits;

   public Pet getPet() { return pet; }
   public Visit getVisit() { return visit; }
   public List<Visit> getVisits() { return visits; }

   public void selectPet(int petId)
   {
      pet = petMaintenance.findById(petId);
   }

   public void selectVisit(int visitId)
   {
      visit = visitMaintenance.findById(visitId);
      pet = visit.getPet();
   }

   public void requestNewVisit()
   {
      visit = new Visit();
   }

   public void createOrUpdateVisit()
   {
      visitMaintenance.create(pet, visit);
   }

   public void showVisits()
   {
      visits = visitMaintenance.findByPetId(pet.getId());
   }
}

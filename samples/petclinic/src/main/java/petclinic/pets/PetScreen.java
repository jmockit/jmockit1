package petclinic.pets;

import java.util.*;
import javax.faces.view.*;
import javax.inject.*;
import javax.transaction.*;

import petclinic.owners.*;

@Named @Transactional @ViewScoped
public class PetScreen
{
   @Inject private PetMaintenance petMaintenance;
   @Inject private OwnerMaintenance ownerMaintenance;
   private List<PetType> types;
   private Owner owner;
   private Pet pet;

   public List<PetType> getTypes()
   {
      if (types == null) {
         types = petMaintenance.findPetTypes();
      }

      return types;
   }

   public Pet getPet() { return pet; }

   public void requestNewPet()
   {
      pet = new Pet();
   }

   public void selectOwner(int ownerId)
   {
      owner = ownerMaintenance.findById(ownerId);
      pet = new Pet();
   }

   public void selectPet(int petId)
   {
      pet = petMaintenance.findById(petId);
   }

   public void createOrUpdatePet()
   {
      if (pet.isNew()) {
         petMaintenance.createPet(owner, pet);
      }
      else {
         petMaintenance.updatePet(pet);
      }
   }
}

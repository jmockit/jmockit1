package petclinic.pets;

import java.util.*;
import javax.annotation.*;
import javax.faces.view.*;
import javax.inject.*;
import javax.transaction.*;

import petclinic.owners.*;

/**
 * An application service class that handles {@link Pet}-related operations from the pet screen.
 */
@Named @Transactional @ViewScoped
public class PetScreen
{
   @Inject private PetMaintenance petMaintenance;
   @Inject private OwnerMaintenance ownerMaintenance;
   @Nullable private List<PetType> types;
   @Nullable private Owner owner;
   @Nullable private Pet pet;

   @Nonnull
   public List<PetType> getTypes()
   {
      if (types == null) {
         types = petMaintenance.findPetTypes();
      }

      return types;
   }

   @Nullable
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
      if (pet != null) {
         if (pet.isNew()) {
            //noinspection ConstantConditions
            petMaintenance.createPet(owner, pet);
         }
         else {
            petMaintenance.updatePet(pet);
         }
      }
   }
}

package petclinic.pets;

import java.util.*;
import javax.inject.*;
import javax.transaction.*;
import javax.validation.*;

import petclinic.owners.*;
import petclinic.util.*;

@Transactional
public class PetMaintenance
{
   @Inject private Database db;

   public Pet findById(int id)
   {
      return db.findById(Pet.class, id);
   }

   /**
    * Finds all pet types.
    *
    * @return the types found, in order of name
    */
   public List<PetType> findPetTypes()
   {
      List<PetType> petTypes = db.find("select t from PetType t order by t.name");
      return petTypes;
   }

   public void createPet(Owner owner, Pet data)
   {
      validate(owner, data);

      data.setOwner(owner);
      owner.addPet(data);
      db.save(data);
   }

   private void validate(Owner owner, Pet pet)
   {
      Pet existingPetOfSameName = owner.getPet(pet.getName());

      if (existingPetOfSameName != null) {
         throw new ValidationException("The owner already has a pet with this name.");
      }
   }

   public void updatePet(Pet data)
   {
      db.save(data);
   }
}

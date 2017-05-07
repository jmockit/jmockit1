package petclinic.pets;

import java.util.*;
import javax.annotation.*;
import javax.inject.*;
import javax.transaction.*;
import javax.validation.*;

import petclinic.owners.*;
import petclinic.util.*;

/**
 * A domain service class for {@link Pet}-related business operations.
 */
@Transactional
public class PetMaintenance
{
   @Inject private Database db;

   @Nullable
   public Pet findById(int id)
   {
      return db.findById(Pet.class, id);
   }

   /**
    * Finds all pet types.
    *
    * @return the types found, in order of name
    */
   @Nonnull
   public List<PetType> findPetTypes()
   {
      List<PetType> petTypes = db.find("select t from PetType t order by t.name");
      return petTypes;
   }

   public void createPet(@Nonnull Owner owner, @Nonnull Pet data)
   {
      validate(owner, data);

      data.setOwner(owner);
      owner.addPet(data);
      db.save(data);
   }

   private void validate(@Nonnull Owner owner, @Nonnull Pet pet)
   {
      Pet existingPetOfSameName = owner.getPet(pet.getName());

      if (existingPetOfSameName != null) {
         throw new ValidationException("The owner already has a pet with this name.");
      }
   }

   public void updatePet(@Nonnull Pet data)
   {
      db.save(data);
   }
}

package petclinic.pets;

import java.util.*;

import javax.inject.*;

import petclinic.owners.*;
import petclinic.util.*;

public final class PetData extends TestDatabase
{
   @Inject private OwnerData ownerData;

   public Pet findOrCreate(String name, Date birthDate, String petType)
   {
      Pet pet = findOne("select p from Pet p where p.name = ?1", name);

      if (pet == null) {
         pet = create(name, birthDate, petType);
      }

      return pet;
   }

   public Pet create(String name, Date birthDate, String petType)
   {
      Owner owner = ownerData.create("Pet Owner");
      return create(owner, name, birthDate, petType);
   }

   public Pet create(Owner owner, String name, Date birthDate, String petType)
   {
      PetType type = findOrCreatePetType(petType);

      Pet pet = new Pet();
      pet.setOwner(owner);
      pet.setName(name);
      pet.setBirthDate(birthDate);
      pet.setType(type);

      db.save(pet);
      return pet;
   }

   PetType findOrCreatePetType(String petType)
   {
      PetType type = findOne("select t from PetType t where t.name = ?1", petType);

      if (type == null) {
         type = createType(petType);
      }

      return type;
   }

   PetType createType(String name)
   {
      PetType type = new PetType();
      type.setName(name);
      db.save(type);
      return type;
   }
}

package petclinic.pets;

import java.util.*;

import javax.annotation.*;
import javax.inject.*;

import petclinic.owners.*;
import petclinic.util.*;

/**
 * Utility class for creation of {@link Pet} data in the test database, to be used in integration tests.
 */
public final class PetData extends TestDatabase
{
   @Inject private OwnerData ownerData;

   @Nonnull
   public Pet findOrCreate(@Nonnull String name, @Nullable Date birthDate, @Nonnull String petType)
   {
      Pet pet = findOne("select p from Pet p where p.name = ?1", name);

      if (pet == null) {
         pet = create(name, birthDate, petType);
      }

      return pet;
   }

   @Nonnull
   public Pet create(@Nonnull String name, @Nullable Date birthDate, @Nonnull String petType)
   {
      Owner owner = ownerData.create("Pet Owner");
      return create(owner, name, birthDate, petType);
   }

   @Nonnull
   public Pet create(@Nonnull Owner owner, @Nonnull String name, @Nullable Date birthDate, @Nonnull String petType)
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

   @Nonnull
   PetType findOrCreatePetType(@Nonnull String petType)
   {
      PetType type = findOne("select t from PetType t where t.name = ?1", petType);

      if (type == null) {
         type = createType(petType);
      }

      return type;
   }

   @Nonnull
   PetType createType(@Nonnull String name)
   {
      PetType type = new PetType();
      type.setName(name);
      db.save(type);
      return type;
   }
}

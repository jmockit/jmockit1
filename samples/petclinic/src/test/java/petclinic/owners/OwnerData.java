package petclinic.owners;

import petclinic.util.*;

/**
 * Utility class for creation of {@link Owner} data in the test database, to be used in integration tests.
 */
public final class OwnerData extends TestDatabase
{
   public Owner create(String fullName)
   {
      String[] names = fullName.split(" ");

      Owner owner = new Owner();
      owner.setFirstName(names[0]);
      owner.setLastName(names[names.length - 1]);
      owner.setTelephone("01234-5678");

      db.save(owner);
      return owner;
   }
}

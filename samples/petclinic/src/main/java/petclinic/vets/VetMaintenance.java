package petclinic.vets;

import java.util.*;
import javax.annotation.*;
import javax.inject.*;
import javax.transaction.*;

import petclinic.util.*;

/**
 * A domain service class for {@link Vet}-related business operations.
 */
@Transactional
public class VetMaintenance
{
   @Inject private Database db;

   @Nonnull
   public List<Vet> findAll()
   {
      return db.find("select v from Vet v order by v.lastName, v.firstName");
   }
}

package petclinic.vets;

import java.util.*;
import javax.inject.*;
import javax.transaction.*;

import petclinic.util.*;

@Transactional
public class VetMaintenance
{
   @Inject private Database db;

   public List<Vet> findAll()
   {
      return db.find("select v from Vet v order by v.lastName, v.firstName");
   }
}

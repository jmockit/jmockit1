package petclinic.visits;

import java.util.*;
import javax.inject.*;

import petclinic.pets.*;
import petclinic.util.*;

public final class VisitData extends TestDatabase
{
   @Inject private PetData petData;

   public Visit create(String description)
   {
      Pet pet = petData.findOrCreate("Test", null, "mouse");

      Visit visit = new Visit();
      visit.setPet(pet);
      visit.setDate(new Date());
      visit.setDescription(description);
      db.save(visit);
      return visit;
   }

   public Visit create()
   {
      return create("Testing");
   }
}

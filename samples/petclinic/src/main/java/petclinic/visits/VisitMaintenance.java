package petclinic.visits;

import java.util.*;
import javax.annotation.*;
import javax.inject.*;
import javax.transaction.*;

import petclinic.pets.*;
import petclinic.util.*;

/**
 * A domain service class for {@link Visit}-related business operations.
 */
@Transactional
public class VisitMaintenance
{
   @Inject private Database db;

   public void create(@Nonnull Pet visitedPet, @Nonnull Visit visitData)
   {
      visitData.setPet(visitedPet);
      visitedPet.addVisit(visitData);
      db.save(visitData);
   }

   @Nullable
   public Visit findById(int visitId)
   {
      Visit visit = db.findById(Visit.class, visitId);
      return visit;
   }

   @Nonnull
   public List<Visit> findByPetId(int petId)
   {
      List<Visit> visits = db.find("select v from Visit v where v.pet.id = ?1", petId);
      return visits;
   }
}

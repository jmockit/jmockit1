package petclinic.visits;

import java.util.*;
import javax.inject.*;
import javax.transaction.*;

import petclinic.pets.*;
import petclinic.util.*;

@Transactional
public class VisitMaintenance
{
   @Inject private Database db;

   public void create(Pet visitedPet, Visit visitData)
   {
      visitData.setPet(visitedPet);
      visitedPet.addVisit(visitData);
      db.save(visitData);
   }

   public Visit findById(int visitId)
   {
      Visit visit = db.findById(Visit.class, visitId);
      return visit;
   }

   public List<Visit> findByPetId(int petId)
   {
      List<Visit> visits = db.find("select v from Visit v where v.pet.id = ?1", petId);
      return visits;
   }
}

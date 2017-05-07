package petclinic.vets;

import java.util.*;
import javax.persistence.*;

import petclinic.util.*;

/**
 * A veterinarian.
 */
@Entity
public class Vet extends Person
{
   @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
   @JoinTable(joinColumns = @JoinColumn(name = "vetId"), inverseJoinColumns = @JoinColumn(name = "specialtyId"))
   @OrderBy("name")
   private final List<Specialty> specialties = new ArrayList<>();

   public List<Specialty> getSpecialties() { return specialties; }
   public int getNrOfSpecialties() { return specialties.size(); }
}

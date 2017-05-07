package petclinic.vets;

import javax.persistence.*;
import javax.validation.constraints.*;

import petclinic.util.*;

/**
 * A {@linkplain Vet Vet's} specialty (for example, "Dentistry").
 */
@Entity
public class Specialty extends BaseEntity
{
   @NotNull
   private String name;

   public String getName() { return name; }
   public void setName(String name) { this.name = name; }
}

package petclinic.pets;

import javax.persistence.*;
import javax.validation.constraints.*;

import petclinic.util.*;

@Entity
public class PetType extends BaseEntity
{
   @NotNull @Size(min = 1)
   private String name;

   public String getName() { return name; }
   public void setName(String name) { this.name = name; }
}

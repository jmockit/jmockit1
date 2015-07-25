package petclinic.util;

import javax.persistence.*;
import javax.validation.constraints.*;

/**
 * A person having a first and a last name.
 */
@MappedSuperclass
public class Person extends BaseEntity
{
   @NotNull @Size(min = 1)
   protected String firstName;

   @NotNull @Size(min = 1)
   protected String lastName;

   public String getFirstName() { return firstName; }
   public void setFirstName(String firstName) { this.firstName = firstName; }

   public String getLastName() { return lastName; }
   public void setLastName(String lastName) { this.lastName = lastName; }
}

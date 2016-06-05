package petclinic.owners;

import java.util.*;
import javax.faces.view.*;
import javax.inject.*;
import javax.transaction.*;

/**
 * An application service class that handles {@link Owner}-related operations from the owner screen.
 */
@Named @Transactional @ViewScoped
public class OwnerScreen
{
   @Inject private OwnerMaintenance ownerMaintenance;
   private String lastName;
   private List<Owner> owners;
   private Owner owner;

   public String getLastName() { return lastName; }
   public void setLastName(String lastName) { this.lastName = lastName; }

   public Owner getOwner() { return owner; }
   public List<Owner> getOwners() { return owners; }

   public void findOwners()
   {
      if (lastName == null) {
         lastName = "";
      }

      owners = ownerMaintenance.findByLastName(lastName);
   }

   public void requestNewOwner()
   {
      owner = new Owner();
   }

   public void selectOwner(int ownerId)
   {
      owner = ownerMaintenance.findById(ownerId);
   }

   public void createOrUpdateOwner()
   {
      ownerMaintenance.createOrUpdate(owner);
   }
}

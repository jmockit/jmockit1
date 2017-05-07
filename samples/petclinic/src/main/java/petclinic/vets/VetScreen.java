package petclinic.vets;

import java.util.*;
import javax.annotation.*;
import javax.faces.view.*;
import javax.inject.*;
import javax.transaction.*;

/**
 * An application service class that handles {@link Vet}-related operations from the vet screen.
 */
@Named @Transactional @ViewScoped
public class VetScreen
{
   @Inject private VetMaintenance vetMaintenance;
   @Nullable private List<Vet> vets;

   @Nullable
   public List<Vet> getVets() { return vets; }

   public void showVetList()
   {
      vets = vetMaintenance.findAll();
   }
}

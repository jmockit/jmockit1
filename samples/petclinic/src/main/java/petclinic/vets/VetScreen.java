package petclinic.vets;

import java.util.*;
import javax.faces.view.*;
import javax.inject.*;
import javax.transaction.*;

@Named @Transactional @ViewScoped
public class VetScreen
{
   @Inject private VetMaintenance vetMaintenance;
   private List<Vet> vets;

   public List<Vet> getVets() { return vets; }

   public void showVetList()
   {
      vets = vetMaintenance.findAll();
   }
}

package petclinic.vets;

import java.util.*;
import static java.util.Arrays.*;

import org.junit.*;
import static org.junit.Assert.*;

import petclinic.util.*;

/**
 * Integration tests for {@link Vet}-related operations, at the application service level.
 * Each test runs in a database transaction that is rolled back at the end of the test.
 */
public final class VetScreenTest
{
   @TestUtil VetData vetData;
   @SUT VetScreen vetScreen;

   @Test
   public void findVets()
   {
      Vet vet2 = vetData.create("Helen Leary", "radiology");
      Vet vet0 = vetData.create("James Carter");
      Vet vet1 = vetData.create("Linda Douglas", "surgery", "dentistry");
      List<Vet> vetsInOrderOfLastName = asList(vet0, vet1, vet2);

      vetScreen.showVetList();
      List<Vet> vets = vetScreen.getVets();

      vets.retainAll(vetsInOrderOfLastName);
      assertEquals(vetsInOrderOfLastName, vets);

      Vet vetWithOneSpecialty = vets.get(2);
      assertEquals(1, vetWithOneSpecialty.getNrOfSpecialties());
      assertEquals("radiology", vetWithOneSpecialty.getSpecialties().get(0).getName());

      Vet vetWithSpecialties = vets.get(1);
      assertEquals(2, vetWithSpecialties.getNrOfSpecialties());

      vetData.refresh(vetWithSpecialties);
      List<Specialty> specialtiesInOrderOfName = vetWithSpecialties.getSpecialties();
      assertEquals("dentistry", specialtiesInOrderOfName.get(0).getName());
      assertEquals("surgery", specialtiesInOrderOfName.get(1).getName());
   }
}

package unitils.wiki;

import org.junit.*;

import static org.junit.Assert.*;
import org.unitils.*;
import org.unitils.inject.annotation.*;
import org.unitils.mock.*;

/**
 * Sample tests extracted from the Unitils Mock
 * <a href="http://sourceforge.net/apps/mediawiki/unitils/index.php?title=Unitils_Mock">wiki</a>.
 */
public final class PartialMockTest extends UnitilsJUnit4
{
   @TestedObject
   MyService myService;

   @InjectIntoByType
   PartialMock<TextService> textService;

   @Test
   public void outputText()
   {
      assertEquals("the text", myService.outputText()); // executes the original behavior
   }

   @Test
   public void outputOtherText()
   {
      textService.returns("some other text").getText(); // overrides the original behavior

      assertEquals("some other text", myService.outputText()); // executes this new behavior
   }
}

package unitils.wiki;

import org.junit.*;

import static org.junit.Assert.*;
import org.unitils.*;
import org.unitils.inject.annotation.*;
import org.unitils.mock.*;
import org.unitils.mock.core.*;

/**
 * Sample tests extracted from the Unitils Mock
 * <a href="http://sourceforge.net/apps/mediawiki/unitils/index.php?title=Unitils_Mock">wiki</a>.
 */
public final class MockChainingTest extends UnitilsJUnit4
{
   @TestedObject
   MyService myService;

   @InjectIntoByType
   Mock<UserService> userService;

   @Test
   public void withoutChaining()
   {
      Mock<User> user = new MockObject<User>("user", User.class, this);

      userService.returns(user).getUser(); // returns the user mock
      user.returns("my name").getName();   // define behavior of user mock

      assertEquals("my name", myService.outputUserName());
   }

   @Test
   public void sameTestButWithChaining()
   {
      userService.returns("my name").getUser().getName(); // automatically returns a user mock

      assertEquals("my name", myService.outputUserName());
   }
}
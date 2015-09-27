package blog.user;

import org.junit.*;
import static org.junit.Assert.*;

import blog.common.*;

public final class UserServiceTest
{
   @TestData UserTestData userData;
   @ObjectUnderTest UserService userService;

   @Test
   public void findExistingByUsername()
   {
      User user = userData.buildAndSave();

      User foundByUsername = userService.findByUsername(user.getUsername());

      assertEquals(user.getId(), foundByUsername.getId());
   }

   @Test
   public void findNonExistingByUsername()
   {
      User user = userService.findByUsername("name");

      assertNull(user);
   }
}

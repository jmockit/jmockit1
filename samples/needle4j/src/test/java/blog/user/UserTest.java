package blog.user;

import javax.persistence.*;

import org.junit.*;
import static org.junit.Assert.*;

import blog.common.*;

public final class UserTest
{
   @TestData UserTestData userData;

   @Test(expected = PersistenceException.class)
   public void attemptToSaveWithDuplicateUsername() {
      userData.withUsername("username").buildAndSave();
      userData.withUsername("username").buildAndSave();
   }

   @Test
   public void verifyPassword() {
      User user = userData.buildAndSave();

      assertFalse(user.getFirstName().isEmpty());
      assertFalse(user.getSurname().isEmpty());
      assertTrue(user.verifyPassword("secret"));
      assertFalse(user.verifyPassword("other"));
   }
}

package blog.security;

import javax.servlet.http.*;

import org.junit.*;
import static org.junit.Assert.*;

import blog.common.*;
import blog.user.*;

public final class AuthenticatorTest
{
   @TestData UserTestData userData;
   @ObjectUnderTest Authenticator authenticator;

   // The following are the same dependencies that get injected into the authenticator;
   // these fields provide access to them so they can be verified.
   @Dependency HttpSession httpSession;
   @Dependency Identity identity;

   @Test
   public void loginWithNoUsername()
   {
      boolean login = authenticator.login();

      assertFalse(login);
      assertFalse(identity.isLoggedIn());
   }

   @Test
   public void loginWithWrongPassword()
   {
      String username = "username";
      User user = userData.withUsername(username).buildAndSave();
      authenticator.setUsername(username);
      authenticator.setPassword("any");

      boolean login = authenticator.login();

      assertFalse(login);
      assertFalse(identity.isLoggedIn());
      assertNotSame(user, identity.getUser());
   }

   @Test
   public void loginSuccess()
   {
      String username = "username";
      User user = userData.withUsername(username).buildAndSave();
      authenticator.setUsername(username);
      String password = "secret";
      authenticator.setPassword(password);

      boolean login = authenticator.login();

      assertTrue(login);
      assertTrue(identity.isLoggedIn());
      assertSame(user, identity.getUser());
      assertEquals(username, authenticator.getUsername());
      assertEquals(password, authenticator.getPassword());
   }

   @Test
   public void logout()
   {
      authenticator.logout();

      try {
         httpSession.invalidate();
         fail("HTTP session was still valid");
      }
      catch (IllegalStateException invalidatedSession) {
         // ok
      }
   }
}

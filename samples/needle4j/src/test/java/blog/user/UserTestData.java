package blog.user;

import javax.annotation.*;
import javax.inject.*;

import blog.common.*;

public final class UserTestData extends BaseTestData<User>
{
   @Inject private User currentUser;
   private String withUsername;

   @PostConstruct
   private void defineCurrentUser()
   {
      currentUser.setFirstName("Test");
      currentUser.setSurname("User");
      currentUser.setUsername("tester");
      currentUser.setPassword("1234");
      save(currentUser);
   }

   public UserTestData withUsername(String username)
   {
      withUsername = username;
      return this;
   }

   @Override
   public User build()
   {
      String username = withUsername != null ? withUsername : "mmuster" + getId();

      User user = new User();
      user.setUsername(username);
      user.setFirstName("Max");
      user.setSurname("Muster");
      user.setPassword("secret");
      return user;
   }
}

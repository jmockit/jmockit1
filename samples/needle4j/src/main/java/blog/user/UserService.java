package blog.user;

import javax.ejb.*;
import javax.inject.*;

import blog.common.*;

@Stateless
public class UserService
{
   @Inject private Database db;

   public User findByUsername(String username)
   {
      return db.findSingle("select u from User u where u.username = ?1", username);
   }
}

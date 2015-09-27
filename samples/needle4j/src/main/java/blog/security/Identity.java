package blog.security;

import java.io.*;
import javax.enterprise.context.*;
import javax.enterprise.inject.*;
import javax.inject.*;

import blog.user.*;

@Named @SessionScoped
public class Identity implements Serializable
{
   private static final long serialVersionUID = 1L;

   private boolean loggedIn;

   @Produces @CurrentUser @Named
   private User currentUser;

   public boolean isLoggedIn() { return loggedIn; }
   public void setLoggedIn(boolean loggedIn) { this.loggedIn = loggedIn; }

   public User getUser() { return currentUser; }
   public void setUser(User user) { currentUser = user; }
}

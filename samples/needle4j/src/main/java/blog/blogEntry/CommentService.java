package blog.blogEntry;

import javax.enterprise.context.*;
import javax.inject.*;

import blog.common.*;
import blog.security.*;
import blog.user.*;

@Named @RequestScoped
public class CommentService
{
   @Inject @CurrentUser private User user;
   @Inject private BlogEntry blogEntry;
   @Inject private Database db;
   private Comment instance;

   public Comment getInstance()
   {
      if (instance == null) {
         instance = new Comment();
         instance.setAuthor(user);
         instance.setBlogEntry(blogEntry);
      }

      return instance;
   }

   public void save()
   {
      db.save(instance);
      instance = null;
   }
}

package blog.blogEntry;

import java.io.*;
import java.util.*;
import javax.enterprise.context.*;
import javax.inject.*;

import blog.common.*;

@Named @ConversationScoped
public class CommentListService implements Serializable
{
   private static final long serialVersionUID = 1L;

   @Inject private BlogEntryService blogEntryService;
   @Inject private Database db;

   public List<Comment> getResultList() {
      BlogEntry blogEntry = blogEntryService.getInstance();
      return db.find("select c from Comment c where c.blogEntry = ?1 order by c.created", blogEntry);
   }
}

package blog.blogEntry;

import java.util.*;
import javax.ejb.*;
import javax.inject.*;

import blog.common.*;

@Stateless
public class BlogEntryDaoBean implements BlogEntryDao
{
   @Inject private Database db;

   @Override
   public List<BlogEntry> find(int maxResults, int firstResult)
   {
      return db.findWithPaging("select b from BlogEntry b order by b.created desc", maxResults, firstResult);
   }

   @Override
   public Long count()
   {
      return db.findSingle("select count(b) from BlogEntry b");
   }
}

package blog.blogEntry;

import java.util.*;
import javax.enterprise.context.*;
import javax.inject.*;

@Named @RequestScoped
public class BlogEntryListService
{
   static final int MAX_RESULTS = 5;

   @Inject private BlogEntryDao blogEntryDao;

   private List<BlogEntry> resultList;
   private int firstResult;

   public List<BlogEntry> getResultList() {
      if (resultList == null) {
         resultList = blogEntryDao.find(MAX_RESULTS, firstResult);
      }

      return resultList;
   }

   public int getNextFirstResult() { return firstResult + MAX_RESULTS; }
   public int getPreviousFirstResult() { return firstResult <= MAX_RESULTS ? 0 : firstResult - MAX_RESULTS; }

   public int getFirstResult() { return firstResult; }

   public void setFirstResult(int firstResult) {
      this.firstResult = firstResult;
      resultList = null;
   }

   public boolean isPreviousExists() { return firstResult > 0; }
   public boolean isNextExists() { return blogEntryDao.count() > MAX_RESULTS + firstResult; }
}

package blog.blogEntry;

import java.util.*;

import org.junit.*;
import static org.junit.Assert.*;

import blog.common.*;
import static blog.blogEntry.BlogEntryListService.MAX_RESULTS;

public final class BlogEntryListServiceTest
{
   @TestData BlogEntryTestData blogEntryData;
   @ObjectUnderTest BlogEntryListService blogEntryListService;

   @Test
   public void getResultList()
   {
      BlogEntry blogEntry = blogEntryData.buildAndSave();

      List<BlogEntry> resultList = blogEntryListService.getResultList();

      assertTrue(resultList.contains(blogEntry));
   }

   @Test
   public void pagination()
   {
      createBlogEntries(MAX_RESULTS + 1);

      assertEquals(0, blogEntryListService.getFirstResult());
      assertEquals(5, blogEntryListService.getNextFirstResult());
      assertEquals(0, blogEntryListService.getPreviousFirstResult());
      assertTrue(blogEntryListService.isNextExists());
      assertFalse(blogEntryListService.isPreviousExists());

      blogEntryListService.setFirstResult(blogEntryListService.getNextFirstResult());

      assertEquals(5, blogEntryListService.getFirstResult());
      assertEquals(10, blogEntryListService.getNextFirstResult());
      assertEquals(0, blogEntryListService.getPreviousFirstResult());
      assertFalse(blogEntryListService.isNextExists());
      assertTrue(blogEntryListService.isPreviousExists());
   }

   void createBlogEntries(int quantity)
   {
      for (int i = 0; i < quantity; i++) {
         blogEntryData.buildAndSave();
      }
   }

   @Test
   public void findEntriesUpToMaximumQuantity()
   {
      createBlogEntries(MAX_RESULTS + 1);

      List<BlogEntry> found = blogEntryListService.getResultList();

      assertEquals(MAX_RESULTS, found.size());
   }

   @Test
   public void previousFirstResult()
   {
      blogEntryListService.setFirstResult(10);

      assertEquals(15, blogEntryListService.getNextFirstResult());
      assertEquals(5, blogEntryListService.getPreviousFirstResult());
   }
}

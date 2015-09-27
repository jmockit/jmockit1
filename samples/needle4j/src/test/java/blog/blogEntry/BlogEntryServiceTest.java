package blog.blogEntry;

import javax.enterprise.context.*;
import javax.validation.*;

import org.junit.*;
import static org.junit.Assert.*;

import blog.common.*;

public final class BlogEntryServiceTest
{
   @TestData BlogEntryTestData blogEntryData;
   @ObjectUnderTest BlogEntryService blogEntryService;
   @Dependency Conversation conversation;

   @Test
   public void beginNewBlogEntry()
   {
      assertTrue(conversation.isTransient());

      blogEntryService.newInstance();
      BlogEntry instance = blogEntryService.getInstance();

      assertNull(instance.getId());
      assertNotNull(instance.getAuthor());
      assertNull(blogEntryService.getId());
      assertFalse(conversation.isTransient());
   }

   @Test
   public void persistNewBlogEntry()
   {
      blogEntryService.newInstance();
      BlogEntry instance = blogEntryService.getInstance();
      String title = "title";
      instance.setTitle(title);
      String content = "content";
      instance.setContent(content);

      blogEntryService.save();

      blogEntryData.assertSavedToDB(instance);
      assertEquals(title, instance.getTitle());
      assertEquals(content, instance.getContent());
      assertEquals(content, instance.getShortContent());
      assertNotNull(instance.getCreated());
      assertNotNull(instance.getVersion());
      assertTrue(instance.getComments().isEmpty());
   }

   @Test(expected = ConstraintViolationException.class)
   public void attemptToSaveWithoutAuthor()
   {
      blogEntryService.newInstance();
      BlogEntry instance = blogEntryService.getInstance();
      instance.setAuthor(null);

      blogEntryService.save();
   }

   @Test
   public void selectBlogEntryById()
   {
      BlogEntry blogEntry = blogEntryData.buildAndSave();
      assertTrue(conversation.isTransient());

      blogEntryService.setId(blogEntry.getId());
      BlogEntry instance = blogEntryService.getInstance();

      assertEquals(blogEntry.getId(), instance.getId());
      String shortContent = instance.getShortContent();
      assertTrue(shortContent.length() < instance.getContent().length());
      assertTrue(shortContent.endsWith("..."));
      assertFalse(conversation.isTransient());
   }

   @Test
   public void selectADifferentBlogEntry()
   {
      BlogEntry blogEntry1 = blogEntryData.withTitle("Blog entry 1").buildAndSave();
      BlogEntry blogEntry2 = blogEntryData.withTitle("Blog entry 2").buildAndSave();

      blogEntryService.setId(blogEntry1.getId());
      BlogEntry instance1 = blogEntryService.getInstance();

      blogEntryService.setId(blogEntry2.getId());
      BlogEntry instance2 = blogEntryService.getInstance();

      assertSame(blogEntry1, instance1);
      assertSame(blogEntry2, instance2);
   }

   @Test
   public void modifyABlogEntry()
   {
      BlogEntry blogEntry = blogEntryData.buildAndSave();
      blogEntryService.setId(blogEntry.getId());
      BlogEntry instance = blogEntryService.getInstance();
      String newContent = "Modified content";

      instance.setContent(newContent);
      blogEntryService.save();

      BlogEntry fromDB = blogEntryData.assertSavedToDB(instance);
      assertEquals(newContent, fromDB.getContent());
      assertEquals(blogEntry.getId(), blogEntryService.getId());
   }

   @Test
   public void deleteSelectedBlogEntry()
   {
      BlogEntry blogEntry = blogEntryData.buildAndSave();
      blogEntryService.setId(blogEntry.getId());
      blogEntryService.getInstance();

      blogEntryService.delete();

      assertTrue(conversation.isTransient());
      blogEntryData.assertDeletedFromDB(blogEntry);
   }

   @Test
   public void deleteSelectedBlogEntry_withDetachedInstance()
   {
      BlogEntry blogEntry = blogEntryData.buildAndSave();
      blogEntryService.setId(blogEntry.getId());
      BlogEntry instance = blogEntryService.getInstance();
      blogEntryData.detachFromPersistenceContext(instance);

      blogEntryService.delete();

      blogEntryData.assertDeletedFromDB(instance);
   }
}

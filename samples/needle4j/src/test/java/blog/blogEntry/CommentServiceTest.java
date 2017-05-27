package blog.blogEntry;

import org.junit.*;
import static org.junit.Assert.*;

import blog.common.*;

public final class CommentServiceTest
{
   @TestData CommentTestData commentData;
   @TestData BlogEntryTestData blogEntryData;
   @Dependency BlogEntry blogEntry;
   @ObjectUnderTest CommentService commentService;

   @Before
   public void createBlogEntry()
   {
      blogEntry = blogEntryData.buildAndSave();
   }

   @Test
   public void createNewPartiallyInitializedComment()
   {
      Comment instance = commentService.getInstance();

      assertNull(instance.getId());
      assertSame(blogEntry, instance.getBlogEntry());
      assertNotNull(instance.getAuthor());
      assertNull(instance.getContent());
      assertNotNull(instance.getCreated());
   }

   @Test
   public void saveNewComment()
   {
      Comment instance = commentService.getInstance();
      instance.setContent("comment");

      commentService.save();

      commentData.assertSavedToDB(instance);
   }
}

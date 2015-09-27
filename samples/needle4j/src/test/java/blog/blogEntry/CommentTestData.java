package blog.blogEntry;

import javax.inject.*;

import blog.common.*;
import blog.user.*;

public final class CommentTestData extends BaseTestData<Comment>
{
   @Inject private UserTestData userData;
   @Inject private BlogEntryTestData blogEntryData;
   private BlogEntry withBlogEntry;

   public CommentTestData withBlogEntry(BlogEntry blogEntry)
   {
      withBlogEntry = blogEntry;
      return this;
   }

   @Override
   public Comment build()
   {
      User author = userData.buildAndSave();
      BlogEntry blogEntry = withBlogEntry != null ? withBlogEntry : blogEntryData.buildAndSave();

      Comment comment = new Comment();
      comment.setAuthor(author);
      comment.setBlogEntry(blogEntry);
      comment.setContent("Lorem ipsum dolor sit amet");
      return comment;
   }
}

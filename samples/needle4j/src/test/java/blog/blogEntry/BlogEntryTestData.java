package blog.blogEntry;

import javax.inject.*;

import blog.common.*;
import blog.user.*;

public final class BlogEntryTestData extends BaseTestData<BlogEntry>
{
   // Injects implementation class instance for injection points having an interface type.
   @Inject private BlogEntryDaoBean dao;

   @Inject private UserTestData userData;
   @Inject private BlogEntry currentBlogEntry;
   private String withTitle;

   public BlogEntryTestData withTitle(String title)
   {
      withTitle = title;
      return this;
   }

   @Override
   public BlogEntry build()
   {
      User author = userData.buildAndSave();
      String title = withTitle != null ? withTitle : "Lorem ipsum dolor sit amet";

      BlogEntry blogEntry = new BlogEntry();
      blogEntry.setAuthor(author);
      blogEntry.setTitle(title);
      blogEntry.setContent(
         "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut " +
         "labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores " +
         "et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. " +
         "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut " +
         "labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores " +
         "et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet");
      return blogEntry;
   }
}

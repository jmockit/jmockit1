package blog.blogEntry;

import java.util.*;
import javax.persistence.*;
import javax.validation.constraints.*;

import blog.common.*;
import blog.user.*;
import static javax.persistence.TemporalType.*;

@Entity
public class Comment extends BaseEntity
{
   private static final long serialVersionUID = 1L;

   @NotNull
   @ManyToOne(optional = false)
   private User author;

   @NotNull
   @ManyToOne(optional = false)
   private BlogEntry blogEntry;

   @Lob
   @Size(min = 1)
   private String content;

   @NotNull
   @Temporal(TIMESTAMP)
   private final Date created = new Date();

   public User getAuthor() { return author; }
   public void setAuthor(User author) { this.author = author; }

   public BlogEntry getBlogEntry() { return blogEntry; }
   public void setBlogEntry(BlogEntry blogEntry) { this.blogEntry = blogEntry; }

   public String getContent() { return content; }
   public void setContent(String content) { this.content = content; }

   public Date getCreated() { return created; }
}

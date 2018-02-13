package blog.blogEntry;

import java.util.*;
import javax.persistence.*;
import javax.validation.constraints.*;

import blog.common.*;
import blog.user.*;
import static javax.persistence.CascadeType.*;
import static javax.persistence.TemporalType.*;

@Entity
public class BlogEntry extends BaseEntity
{
   private static final long serialVersionUID = 1L;

   @Column(nullable = false)
   @Size(min = 1)
   private String title;

   @Lob
   @Column(length = 2000)
   @Size(min = 1)
   private String content;

   @NotNull
   @ManyToOne(optional = false)
   private User author;

   @NotNull
   @Temporal(TIMESTAMP)
   private final Date created = new Date();

   @OneToMany(mappedBy = "blogEntry", cascade = ALL)
   private final List<Comment> comments = new ArrayList<>();

   public String getTitle() { return title; }
   public void setTitle(String title) { this.title = title; }

   public String getContent() { return content; }
   public void setContent(String content) { this.content = content; }

   public String getShortContent() {
      if (content != null && content.length() > 200) {
         return content.substring(0, 200) + "...";
      }

      return content;
   }

   public User getAuthor() { return author; }
   public void setAuthor(User author) { this.author = author; }

   public Date getCreated() { return created; }

   public List<Comment> getComments() { return comments; }
}

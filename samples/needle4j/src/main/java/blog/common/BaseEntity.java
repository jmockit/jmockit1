package blog.common;

import java.io.*;
import java.sql.*;
import java.util.Date;
import javax.persistence.*;

import static javax.persistence.GenerationType.*;

@MappedSuperclass
public class BaseEntity implements Serializable
{
   private static final long serialVersionUID = 1L;

   @Id
   @GeneratedValue(strategy = IDENTITY)
   private Long id;

   @Version
   private Timestamp version;

   public Long getId() { return id; }
   public Date getVersion() { return version; }
}

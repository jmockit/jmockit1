package blog.user;

import javax.persistence.*;
import javax.validation.constraints.*;

import blog.common.*;
import org.jasypt.util.password.*;

@Entity
public class User extends BaseEntity
{
   private static final long serialVersionUID = 1L;

   @Column(nullable = false)
   @Size(min = 1)
   private String firstName;

   @Column(nullable = false)
   @Size(min = 1)
   private String surname;

   @Column(nullable = false, unique = true)
   @Size(min = 1)
   private String username;

   @Column(nullable = false)
   @Size(min = 1)
   private String password;

   public String getFirstName() { return firstName; }
   public void setFirstName(String firstName) { this.firstName = firstName; }

   public String getSurname() { return surname; }
   public void setSurname(String surname) { this.surname = surname; }

   public String getUsername() { return username; }
   public void setUsername(String username) { this.username = username; }

   public void setPassword(String password) {
      this.password = new BasicPasswordEncryptor().encryptPassword(password);
   }

   public boolean verifyPassword(String passwordToVerify) {
      return new BasicPasswordEncryptor().checkPassword(passwordToVerify, password);
   }
}

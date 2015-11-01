package mockmvc.example;

import org.springframework.validation.*;

public class CommentValidator implements Validator
{
   @Override
   public boolean supports(Class<?> clazz)
   {
      return false;
   }

   @Override
   public void validate(Object target, Errors errors)
   {
   }
}

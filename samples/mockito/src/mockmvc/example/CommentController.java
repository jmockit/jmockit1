package mockmvc.example;

import javax.validation.*;

import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.ui.*;
import org.springframework.validation.*;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.*;

@Controller @RequestMapping("/comment/{uuid}")
public class CommentController
{
   @Autowired private RequestService requestService;
   @Autowired private CommentValidator validator;

   @InitBinder("commentForm")
   protected void initBinder(WebDataBinder binder)
   {
      binder.setValidator(validator);
   }

   @RequestMapping(method = RequestMethod.POST)
   public String saveComment(
      @PathVariable String uuid, @Valid @ModelAttribute CommentForm commentForm, BindingResult result, Model model)
   {
      RequestComment requestComment = requestService.getRequestCommentByUUID(uuid);

      if (requestComment == null) {
         return "redirect:/dashboard";
      }

      if (result.hasErrors()) {
         return "comment";
      }

      return "ok";
   }
}
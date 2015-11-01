package mockmvc.example;

import org.junit.*;

import mockit.*;

import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.*;
import org.springframework.validation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public final class StandaloneJMockitTest
{
   @Injectable RequestService requestService;
   @Injectable CommentValidator validator;
   @Tested(availableDuringSetup = true) CommentController commentController;
   MockMvc mockMvc;

   @Before
   public void setup()
   {
      mockMvc = MockMvcBuilders.standaloneSetup(commentController).build();
      new Expectations() {{ validator.supports((Class<?>) any); result = true; }};
   }

   @Test
   public void saveCommentWhenRequestCommentIsNotFound() throws Exception
   {
      new Expectations() {{ requestService.getRequestCommentByUUID("123"); result = null; }};

      mockMvc.perform(post("/comment/{uuid}", "123"))
         .andExpect(status().isFound())
         .andExpect(view().name("redirect:/dashboard"));
   }

   @Test
   public void saveCommentWhenThereIsAFormError() throws Exception
   {
      new Expectations() {{
         requestService.getRequestCommentByUUID("123"); result = new RequestComment();

         validator.validate(any, (Errors) any);
         result = new Delegate() {
            @SuppressWarnings("unused")
            void validate(Object target, Errors errors) { errors.reject("forcing some error"); }
         };
      }};

      mockMvc.perform(post("/comment/{uuid}", "123"))
         .andExpect(status().isOk())
         .andExpect(view().name("comment"));
   }

   @Test
   public void saveComment() throws Exception
   {
      new Expectations() {{ requestService.getRequestCommentByUUID("123"); result = new RequestComment(); }};

      mockMvc.perform(post("/comment/{uuid}", "123"))
         .andExpect(status().isOk())
         .andExpect(view().name("ok"));
   }
}
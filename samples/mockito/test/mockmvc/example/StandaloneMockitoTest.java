package mockmvc.example;

import org.junit.*;

import org.mockito.*;
import org.mockito.invocation.*;
import org.mockito.stubbing.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.*;
import org.springframework.validation.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public final class StandaloneMockitoTest
{
   @Mock RequestService requestService;
   @Mock CommentValidator validator;
   @InjectMocks CommentController commentController;
   MockMvc mockMvc;

   @Before
   public void setup()
   {
      MockitoAnnotations.initMocks(this);
      mockMvc = MockMvcBuilders.standaloneSetup(commentController).build();
      when(validator.supports(any(Class.class))).thenReturn(true);
   }

   @Test
   public void saveCommentWhenRequestCommentIsNotFound() throws Exception
   {
      when(requestService.getRequestCommentByUUID("123")).thenReturn(null);

      mockMvc.perform(post("/comment/{uuid}", "123"))
         .andExpect(status().isFound())
         .andExpect(view().name("redirect:/dashboard"));
   }

   @Test
   public void saveCommentWhenThereIsAFormError() throws Exception
   {
      when(requestService.getRequestCommentByUUID("123")).thenReturn(new RequestComment());

      doAnswer(new Answer() {
         @Override
         public Object answer(InvocationOnMock invocationOnMock) throws Throwable
         {
            Errors errors = (Errors) invocationOnMock.getArguments()[1];
            errors.reject("forcing some error");
            return null;
         }
      }).when(validator).validate(anyObject(), any(Errors.class));

      mockMvc.perform(post("/comment/{uuid}", "123"))
         .andExpect(status().isOk())
         .andExpect(view().name("comment"));
   }

   @Test
   public void saveComment() throws Exception
   {
      when(requestService.getRequestCommentByUUID("123")).thenReturn(new RequestComment());

      mockMvc.perform(post("/comment/{uuid}", "123"))
         .andExpect(status().isOk())
         .andExpect(view().name("ok"));
   }
}
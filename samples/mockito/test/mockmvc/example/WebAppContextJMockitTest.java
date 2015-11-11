package mockmvc.example;

import org.junit.*;
import org.junit.runner.*;

import mockit.*;

import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.test.context.*;
import org.springframework.test.context.junit4.*;
import org.springframework.test.context.web.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.*;
import org.springframework.validation.*;
import org.springframework.web.context.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The same tests as in {@link StandaloneJMockitTest}, but creating the {@link MockMvc} object with a call to
 * {@link MockMvcBuilders#webAppContextSetup(WebApplicationContext)} instead of
 * {@link MockMvcBuilders#standaloneSetup(Object...)}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = WebAppContextJMockitTest.WebAppConfig.class)
public final class WebAppContextJMockitTest
{
   @Configuration
   @ComponentScan(excludeFilters = @ComponentScan.Filter(value = Configuration.class, type = FilterType.ANNOTATION))
   static class WebAppConfig
   {
      @Bean
      RequestService requestService() {
         return new RequestService() {
            @Override
            public RequestComment getRequestCommentByUUID(String uuid) { return null; }
         };
      }

      @Bean CommentValidator validator() { return new CommentValidator(); }
   }

   // These two fields provide access to the beans created in the configuration, as mocked instances.
   @Capturing RequestService requestService;
   @Capturing CommentValidator validator;

   @Autowired WebApplicationContext context;
   MockMvc mockMvc;

   @Before
   public void setup()
   {
      mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

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

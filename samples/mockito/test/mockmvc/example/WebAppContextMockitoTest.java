package mockmvc.example;

import org.junit.*;
import org.junit.runner.*;

import org.mockito.invocation.*;
import org.mockito.stubbing.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.test.context.*;
import org.springframework.test.context.junit4.*;
import org.springframework.test.context.web.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.*;
import org.springframework.validation.*;
import org.springframework.web.context.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The same tests as in {@link StandaloneMockitoTest}, but creating the {@link MockMvc} object with a call to
 * {@link MockMvcBuilders#webAppContextSetup(WebApplicationContext)} instead of
 * {@link MockMvcBuilders#standaloneSetup(Object...)}.
 * <p/>
 * This version loads a Spring application context, which defines the beans on which the controller under test
 * depends.
 * <p/>
 * Using a configured context is significantly more complex than just letting Mockito instantiate the controller and its
 * mock dependencies, as it adds several unique requirements:
 * <ol type="a">
 *     <li>use of {@link SpringJUnit4ClassRunner} (or equivalent mechanism),
 *     <li>addition of {@code @WebAppConfiguration} <em>and</em> {@code @ContextConfiguration} to the test class,
 *     <li>addition of a {@link WebApplicationContext} field to the test class,
 *     <li>creation of an XML or Java configuration with a suitable bean for each dependency, and
 *     <li>a necessary call to {@link org.mockito.Mockito#reset(Object[])} in a test setup method.
 * </ol>
 * For all this hard work, you get a real Spring context that can handle, if so configured, annotations such as
 * {@code @Transactional} and {@code @Cached}. To prevent Spring from instantiating all (transitive) dependencies
 * (even of your mocks), you need to put a {@code FactoryBean<MyMock>} into the context instead of a {@code MyMock}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = WebAppContextMockitoTest.WebAppConfig.class)
public final class WebAppContextMockitoTest
{
   @Configuration
   @ComponentScan(excludeFilters = @ComponentScan.Filter(value = Configuration.class, type = FilterType.ANNOTATION))
   static class WebAppConfig
   {
      @Bean RequestService requestService() { return mock(RequestService.class); }
      @Bean CommentValidator validator() { return mock(CommentValidator.class); }
   }

   // These two fields provide access to the mock beans created in the configuration.
   @Autowired RequestService requestService;
   @Autowired CommentValidator validator;

   @Autowired WebApplicationContext context;
   MockMvc mockMvc;

   @Before
   public void setup()
   {
      mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

      reset(requestService, validator); // without this call, the last test fails
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
         public Object answer(InvocationOnMock invocation) {
            Errors errors = (Errors) invocation.getArguments()[1];
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

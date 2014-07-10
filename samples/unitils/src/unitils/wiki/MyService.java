package unitils.wiki;

public final class MyService
{
   private final TextService textService = new TextService();
   private final UserService userService = new UserService();

   public String outputText()
   {
      return textService.getText();
   }

   public String outputUserName()
   {
      return userService.getUser().getName();
   }
}

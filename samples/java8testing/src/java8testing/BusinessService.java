package java8testing;

public final class BusinessService
{
   private final Collaborator collaborator;

   public BusinessService(Collaborator collaborator) {
      this.collaborator = collaborator;
   }

   public Collaborator getCollaborator() { return collaborator; }

   public String performBusinessOperation(int value) {
      return collaborator.doSomething(value + 1);
   }
}

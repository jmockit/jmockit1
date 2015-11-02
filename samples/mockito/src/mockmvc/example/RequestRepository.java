package mockmvc.example;

public interface RequestRepository
{
   RequestComment findByUUID(String uuid);
}

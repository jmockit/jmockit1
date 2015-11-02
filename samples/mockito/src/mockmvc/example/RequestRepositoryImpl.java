package mockmvc.example;

import org.springframework.stereotype.*;

@Repository
public final class RequestRepositoryImpl implements RequestRepository
{
   @Override
   public RequestComment findByUUID(String uuid) { return null; }
}

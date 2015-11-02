package mockmvc.example;

import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

@Service
public final class RequestServiceImpl implements RequestService
{
   @Autowired private RequestRepository repository;

   @Override
   public RequestComment getRequestCommentByUUID(String uuid)
   {
      return repository.findByUUID(uuid);
   }
}

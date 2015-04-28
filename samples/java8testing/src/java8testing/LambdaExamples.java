package java8testing;

import java.util.*;
import static java.util.stream.Collectors.*;

public final class LambdaExamples
{
   public static class Entity {}

   public <E extends Entity> E loadEntity(Class<E> entityClass, Integer id)
   {
      //noinspection unchecked
      return (E) new Entity();
   }

   public <E extends Entity> List<E> noLambdas(Class<E> entityClass, List<Integer> ids)
   {
      List<E> entities = new ArrayList<>();

      for (Integer id : ids) {
         if (id > 0) {
            E entity = loadEntity(entityClass, id);
            entities.add(entity);
         }
      }

      return entities;
   }

   public <E extends Entity> List<E> withLambdas(Class<E> entityClass, List<Integer> ids)
   {
      List<E> entities = ids.stream().filter(id -> id > 0).map(id -> loadEntity(entityClass, id)).collect(toList());
      return entities;
   }
}

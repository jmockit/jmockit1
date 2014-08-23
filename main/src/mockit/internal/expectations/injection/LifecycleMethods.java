package mockit.internal.expectations.injection;

import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;

import mockit.internal.util.*;

import org.jetbrains.annotations.*;

final class LifecycleMethods
{
   private final Map<Class<?>, Method> postConstructMethods;
   private final Map<Class<?>, Method> preDestroyMethods;
   private final List<Object> objectsWithPreDestroyMethodsToExecute;

   LifecycleMethods()
   {
      postConstructMethods = new IdentityHashMap<Class<?>, Method>();
      preDestroyMethods = new IdentityHashMap<Class<?>, Method>();
      objectsWithPreDestroyMethodsToExecute = new ArrayList<Object>();
   }

   private void findLifecycleMethods(@NotNull Class<?> testedClass)
   {
      Method postConstructMethod = null;
      Method preDestroyMethod = null;

      for (Method method : testedClass.getDeclaredMethods()) {
         if (postConstructMethod == null && method.isAnnotationPresent(PostConstruct.class)) {
            postConstructMethod = method;
         }
         else if (preDestroyMethod == null && method.isAnnotationPresent(PreDestroy.class)) {
            preDestroyMethod = method;
         }

         if (postConstructMethod != null && preDestroyMethod != null) {
            break;
         }
      }

      postConstructMethods.put(testedClass, postConstructMethod);
      preDestroyMethods.put(testedClass, preDestroyMethod);
   }

   void executePostConstructMethodIfAny(@NotNull Class<?> testedClass, @NotNull Object testedObject)
   {
      Method postConstructMethod = getLifecycleMethod(testedClass);

      if (postConstructMethod != null) {
         MethodReflection.invoke(testedObject, postConstructMethod);
      }

      Method preDestroyMethod = preDestroyMethods.get(testedClass);

      if (preDestroyMethod != null) {
         objectsWithPreDestroyMethodsToExecute.add(testedObject);
      }
   }

   @Nullable
   private Method getLifecycleMethod(@NotNull Class<?> testedClass)
   {
      if (!postConstructMethods.containsKey(testedClass)) {
         findLifecycleMethods(testedClass);
      }

      return postConstructMethods.get(testedClass);
   }

   void executePreDestroyMethodsIfAny()
   {
      for (Object testedObject : objectsWithPreDestroyMethodsToExecute) {
         Class<?> testedClass = testedObject.getClass();
         Method preDestroyMethod = preDestroyMethods.get(testedClass);

         try { MethodReflection.invoke(testedObject, preDestroyMethod); }
         catch (RuntimeException ignore) {}
         catch (AssertionError ignore) {}
      }
   }
}

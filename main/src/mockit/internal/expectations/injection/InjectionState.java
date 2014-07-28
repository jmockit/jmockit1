package mockit.internal.expectations.injection;

import java.lang.reflect.*;
import java.util.*;

import javax.inject.*;

import mockit.internal.expectations.mocking.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

import org.jetbrains.annotations.*;

/**
 * Holds state used throughout the injection process while it's in progress for a given set of tested objects.
 */
final class InjectionState
{
   private GenericTypeReflection testedTypeReflection;
   private Type typeOfInjectionPoint;
   @NotNull private List<MockedType> injectables;
   @NotNull private List<MockedType> consumedInjectables;
   private Object currentTestClassInstance;

   InjectionState()
   {
      injectables = Collections.emptyList();
      consumedInjectables = new ArrayList<MockedType>();
   }

   void buildListsOfInjectables(@NotNull Object testClassInstance, @NotNull List<MockedType> injectableFields)
   {
      currentTestClassInstance = testClassInstance;
      ParameterTypeRedefinitions paramTypeRedefs = TestRun.getExecutingTest().getParameterTypeRedefinitions();

      if (paramTypeRedefs == null) {
         injectables = injectableFields;
      }
      else {
         injectables = new ArrayList<MockedType>(injectableFields);
         injectables.addAll(paramTypeRedefs.getInjectableParameters());
      }
   }

   public Object getCurrentTestClassInstance() { return currentTestClassInstance; }

   void setTestedField(@NotNull Field testedField) { testedTypeReflection = new GenericTypeReflection(testedField); }

   void setTypeOfInjectionPoint(@NotNull Type typeOfInjectionPoint)
   {
      this.typeOfInjectionPoint = typeOfInjectionPoint;
   }

   private boolean hasSameTypeAsInjectionPoint(@NotNull MockedType injectable)
   {
      return isSameTypeAsInjectionPoint(injectable.declaredType);
   }

   boolean isSameTypeAsInjectionPoint(@NotNull Type injectableType)
   {
      if (testedTypeReflection.areMatchingTypes(typeOfInjectionPoint, injectableType)) {
         return true;
      }

      if (InjectionPoint.INJECT_CLASS != null && typeOfInjectionPoint instanceof ParameterizedType) {
         ParameterizedType parameterizedType = (ParameterizedType) typeOfInjectionPoint;

         if (parameterizedType.getRawType() == Provider.class) {
            Type providedType = parameterizedType.getActualTypeArguments()[0];
            return providedType.equals(injectableType);
         }
      }

      return false;
   }

   @Nullable
   MockedType findNextInjectableForInjectionPoint()
   {
      for (MockedType injectable : injectables) {
         if (hasSameTypeAsInjectionPoint(injectable) && !consumedInjectables.contains(injectable)) {
            return injectable;
         }
      }

      return null;
   }

   @Nullable
   MockedType findInjectableByTypeAndOptionallyName(@NotNull String nameOfInjectionPoint)
   {
      MockedType found = null;

      for (MockedType injectable : injectables) {
         if (hasSameTypeAsInjectionPoint(injectable)) {
            if (nameOfInjectionPoint.equals(injectable.mockId)) {
               return injectable;
            }

            if (found == null) {
               found = injectable;
            }
         }
      }

      return found;
   }

   @Nullable
   MockedType findInjectableByTypeAndName(@NotNull String nameOfInjectionPoint)
   {
      for (MockedType injectable : injectables) {
         if (hasSameTypeAsInjectionPoint(injectable) && nameOfInjectionPoint.equals(injectable.mockId)) {
            return injectable;
         }
      }

      return null;
   }

   @Nullable
   Object getValueToInject(@NotNull MockedType injectable)
   {
      if (consumedInjectables.contains(injectable)) {
         return null;
      }

      Object value = injectable.getValueToInject(currentTestClassInstance);

      if (value != null) {
         consumedInjectables.add(injectable);
      }

      return value;
   }

   void resetConsumedInjectables()
   {
      consumedInjectables.clear();
   }

   @NotNull
   List<MockedType> saveConsumedInjectables()
   {
      List<MockedType> previousConsumedInjectables = consumedInjectables;
      consumedInjectables = new ArrayList<MockedType>();
      return previousConsumedInjectables;
   }

   void restoreConsumedInjectables(@NotNull List<MockedType> previousConsumedInjectables)
   {
      consumedInjectables = previousConsumedInjectables;
   }
}

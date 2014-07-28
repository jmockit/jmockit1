package mockit.internal.expectations.injection;

import java.lang.reflect.*;
import java.util.*;
import static java.lang.reflect.Modifier.*;

import mockit.*;
import mockit.internal.util.*;
import static mockit.internal.expectations.injection.InjectionPoint.*;

import org.jetbrains.annotations.*;

final class TestedField
{
   @NotNull private final InjectionState injectionState;
   @NotNull private final Field testedField;
   @NotNull private final Tested metadata;
   @NotNull private final TestedObjectCreation testedObjectCreation;
   @Nullable private List<Field> targetFields;
   private boolean createAutomatically;

   TestedField(@NotNull InjectionState injectionState, @NotNull Field field, @NotNull Tested metadata)
   {
      this.injectionState = injectionState;
      testedField = field;
      this.metadata = metadata;
      testedObjectCreation = new TestedObjectCreation(injectionState, field);
   }

   void instantiateWithInjectableValues(@NotNull Object testClassInstance)
   {
      Object testedObject = null;

      if (!createAutomatically) {
         testedObject = FieldReflection.getFieldValue(testedField, testClassInstance);
         createAutomatically = testedObject == null && !isFinal(testedField.getModifiers());
      }

      injectionState.setTestedField(testedField);

      boolean requiresAnnotation = false;
      Class<?> testedClass;

      if (createAutomatically) {
         testedClass = testedField.getType();
         testedObject = testedObjectCreation.create();
         FieldReflection.setFieldValue(testedField, testClassInstance, testedObject);
         requiresAnnotation = testedObjectCreation.constructorIsAnnotated;
      }
      else {
         testedClass = testedObject == null ? null : testedObject.getClass();
      }

      if (testedObject != null) {
         FieldInjection fieldInjection =
            new FieldInjection(
               injectionState, testedClass, testedObject, requiresAnnotation, metadata.fullyInitialized());

         if (targetFields == null) {
            targetFields = fieldInjection.findAllTargetInstanceFieldsInTestedClassHierarchy();
         }

         fieldInjection.injectIntoEligibleFields(targetFields);

         if (createAutomatically) {
            executePostConstructMethodIfAny(testedClass, testedObject);
         }
      }
   }

   void clearIfAutomaticCreation()
   {
      if (createAutomatically) {
         Object testClassInstance = injectionState.getCurrentTestClassInstance();
         Object testedObject = FieldReflection.getFieldValue(testedField, testClassInstance);

         if (testedObject != null) {
            Class<?> testedClass = testedField.getType();
            executePreDestroyMethodIfAny(testedClass, testedObject);
         }

         FieldReflection.setFieldValue(testedField, testClassInstance, null);
      }
   }
}

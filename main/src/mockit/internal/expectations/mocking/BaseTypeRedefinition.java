/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.lang.instrument.*;
import java.lang.reflect.*;
import java.lang.reflect.Type;
import java.util.*;

import static java.lang.reflect.Modifier.*;

import org.jetbrains.annotations.*;

import mockit.external.asm4.*;
import mockit.internal.*;
import mockit.internal.expectations.mocking.InstanceFactory.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

abstract class BaseTypeRedefinition
{
   private static final class MockedClass
   {
      @Nullable final InstanceFactory instanceFactory;
      @NotNull final ClassDefinition[] mockedClassDefinitions;

      MockedClass(@Nullable InstanceFactory instanceFactory, @NotNull ClassDefinition[] classDefinitions)
      {
         this.instanceFactory = instanceFactory;
         mockedClassDefinitions = classDefinitions;
      }

      void redefineClasses()
      {
         RedefinitionEngine.redefineClasses(mockedClassDefinitions);
      }
   }

   private static final Map<Integer, MockedClass> mockedClasses = new HashMap<Integer, MockedClass>();
   private static final Map<Type, Class<?>> mockInterfaces = new HashMap<Type, Class<?>>();

   @NotNull Class<?> targetClass;
   @NotNull MockedType typeMetadata;
   @Nullable private InstanceFactory instanceFactory;
   @Nullable private List<ClassDefinition> mockedClassDefinitions;

   BaseTypeRedefinition(@NotNull MockedType typeMetadata)
   {
      targetClass = typeMetadata.getClassType();
      this.typeMetadata = typeMetadata;
   }

   @NotNull
   final InstanceFactory redefineType(@NotNull Type typeToMock)
   {
      if (targetClass == TypeVariable.class || targetClass.isInterface()) {
         createMockedInterfaceImplementationAndInstanceFactory(typeToMock);
      }
      else {
         redefineTargetClassAndCreateInstanceFactory(typeToMock);
      }

      assert instanceFactory != null;
      TestRun.mockFixture().registerInstanceFactoryForMockedType(targetClass, instanceFactory);
      return instanceFactory;
   }

   private void createMockedInterfaceImplementationAndInstanceFactory(@NotNull Type interfaceToMock)
   {
      Class<?> mockedInterface = interfaceToMock(interfaceToMock);

      if (mockedInterface == null) {
         createMockInterfaceImplementationUsingStandardProxy(interfaceToMock);
         return;
      }

      Class<?> mockClass = mockInterfaces.get(interfaceToMock);

      if (mockClass != null) {
         targetClass = mockClass;
         createNewMockInstanceFactoryForInterface();
         return;
      }

      generateNewMockImplementationClassForInterface(interfaceToMock);
      createNewMockInstanceFactoryForInterface();

      mockInterfaces.put(interfaceToMock, targetClass);
   }

   @Nullable
   private static Class<?> interfaceToMock(@NotNull Type typeToMock)
   {
      while (true) {
         if (typeToMock instanceof Class<?>) {
            Class<?> theInterface = (Class<?>) typeToMock;

            if (isPublic(theInterface.getModifiers()) && !theInterface.isAnnotation()) {
               return theInterface;
            }
         }
         else if (typeToMock instanceof ParameterizedType) {
            typeToMock = ((ParameterizedType) typeToMock).getRawType();
            continue;
         }

         return null;
      }
   }

   private void createMockInterfaceImplementationUsingStandardProxy(@NotNull Type typeToMock)
   {
      Object mock = EmptyProxy.Impl.newEmptyProxy(getClass().getClassLoader(), typeToMock);
      targetClass = mock.getClass();

      redefineMethodsAndConstructorsInTargetType();

      instanceFactory = new InterfaceInstanceFactory(mock);
   }

   private void createNewMockInstanceFactoryForInterface()
   {
      Object mock = ConstructorReflection.newInstanceUsingDefaultConstructor(targetClass);
      instanceFactory = new InterfaceInstanceFactory(mock);
   }

   private void generateNewMockImplementationClassForInterface(@NotNull final Type interfaceToMock)
   {
      targetClass = new ImplementationClass(interfaceToMock) {
         @Override
         @NotNull
         protected ClassVisitor createMethodBodyGenerator(@NotNull ClassReader typeReader, @NotNull String className)
         {
            return new InterfaceImplementationGenerator(typeReader, interfaceToMock, className);
         }
      }.generateNewMockImplementationClassForInterface();
   }

   final void redefineMethodsAndConstructorsInTargetType()
   {
      redefineClassAndItsSuperClasses(targetClass);
   }

   private void redefineClassAndItsSuperClasses(@NotNull Class<?> realClass)
   {
      ClassReader classReader = createClassReader(realClass);
      ExpectationsModifier modifier = new ExpectationsModifier(realClass.getClassLoader(), classReader, typeMetadata);

      try {
         redefineClass(realClass, classReader, modifier);
      }
      catch (VisitInterruptedException ignore) {
         // As defined in ExpectationsModifier, some critical JRE classes have all methods excluded from mocking by
         // default. This exception occurs when they are visited.
         // In this case, we simply stop class redefinition for the rest of the class hierarchy.
         return;
      }

      Class<?> superClass = realClass.getSuperclass();

      if (superClass != null && superClass != Object.class && superClass != Proxy.class && superClass != Enum.class) {
         redefineClassAndItsSuperClasses(superClass);
      }
   }

   private void redefineClass(
      @NotNull Class<?> realClass, @NotNull ClassReader classReader, @NotNull ClassVisitor modifier)
   {
      classReader.accept(modifier, ClassReader.SKIP_FRAMES);
      byte[] modifiedClass = modifier.toByteArray();

      ClassDefinition classDefinition = new ClassDefinition(realClass, modifiedClass);
      RedefinitionEngine.redefineClasses(classDefinition);

      if (mockedClassDefinitions != null) {
         mockedClassDefinitions.add(classDefinition);
      }
   }

   @NotNull
   private static ClassReader createClassReader(@NotNull Class<?> realClass)
   {
      return ClassFile.createReaderOrGetFromCache(realClass);
   }

   private void redefineTargetClassAndCreateInstanceFactory(@NotNull Type typeToMock)
   {
      Integer mockedClassId = redefineClassesFromCache();

      if (mockedClassId == null) {
         return;
      }

      if (targetClass.isEnum()) {
         instanceFactory = new EnumInstanceFactory(targetClass);
         redefineMethodsAndConstructorsInTargetType();
      }
      else if (isAbstract(targetClass.getModifiers())) {
         redefineMethodsAndConstructorsInTargetType();
         Class<?> subclass = generateConcreteSubclassForAbstractType(typeToMock);
         instanceFactory = new ClassInstanceFactory(subclass);
      }
      else {
         redefineMethodsAndConstructorsInTargetType();
         instanceFactory = new ClassInstanceFactory(targetClass);
      }

      storeRedefinedClassesInCache(mockedClassId);
   }

   @Nullable
   final Integer redefineClassesFromCache()
   {
      Integer mockedClassId = typeMetadata.hashCode();
      MockedClass mockedClass = mockedClasses.get(mockedClassId);

      if (mockedClass != null) {
         mockedClass.redefineClasses();
         instanceFactory = mockedClass.instanceFactory;
         return null;
      }

      mockedClassDefinitions = new ArrayList<ClassDefinition>();
      return mockedClassId;
   }

   final void storeRedefinedClassesInCache(@NotNull Integer mockedClassId)
   {
      assert mockedClassDefinitions != null;
      ClassDefinition[] classDefs = mockedClassDefinitions.toArray(new ClassDefinition[mockedClassDefinitions.size()]);
      MockedClass mockedClass = new MockedClass(instanceFactory, classDefs);

      mockedClasses.put(mockedClassId, mockedClass);
   }

   @NotNull
   private Class<?> generateConcreteSubclassForAbstractType(@NotNull Type typeToMock)
   {
      ClassReader classReader = createClassReader(targetClass);
      String subclassName = getNameForConcreteSubclassToCreate();

      SubclassGenerationModifier modifier =
         new SubclassGenerationModifier(typeMetadata.mockingCfg, typeToMock, classReader, subclassName);
      classReader.accept(modifier, ClassReader.SKIP_FRAMES);
      byte[] bytecode = modifier.toByteArray();

      return ImplementationClass.defineNewClass(targetClass.getClassLoader(), bytecode, subclassName);
   }

   @NotNull
   abstract String getNameForConcreteSubclassToCreate();
}
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

import mockit.external.asm4.*;
import mockit.internal.*;
import mockit.internal.expectations.mocking.InstanceFactory.*;
import mockit.internal.state.*;
import mockit.internal.util.*;
import static mockit.external.asm4.ClassReader.*;
import static mockit.internal.util.Utilities.*;

import org.jetbrains.annotations.*;

class BaseTypeRedefinition
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
   private static final Map<Type, Class<?>> mockImplementations = new HashMap<Type, Class<?>>();

   @NotNull Class<?> targetClass;
   @NotNull MockedType typeMetadata;
   @Nullable private InstanceFactory instanceFactory;
   @Nullable private List<ClassDefinition> mockedClassDefinitions;

   BaseTypeRedefinition() {}

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

      Class<?> previousMockImplementationClass = mockImplementations.get(interfaceToMock);

      if (previousMockImplementationClass == null) {
         generateNewMockImplementationClassForInterface(interfaceToMock);
         mockImplementations.put(interfaceToMock, targetClass);
      }
      else {
         targetClass = previousMockImplementationClass;
      }

      redefinedImplementedInterfacesIfRunningOnJava8(targetClass);
      createNewMockInstanceFactoryForInterface();
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
      ClassLoader loader = getClass().getClassLoader();
      Object mock = EmptyProxy.Impl.newEmptyProxy(loader, typeToMock);
      targetClass = mock.getClass();

      redefineClass(targetClass);

      instanceFactory = new InterfaceInstanceFactory(mock);
   }

   private void redefineClass(@NotNull Class<?> realClass)
   {
      ClassLoader loader = realClass.getClassLoader();
      ClassReader classReader = ClassFile.createReaderOrGetFromCache(realClass);
      ClassVisitor modifier = createClassModifier(loader, classReader);
      redefineClass(realClass, classReader, modifier);
   }

   private ExpectationsModifier createClassModifier(@NotNull ClassLoader loader, @NotNull ClassReader classReader)
   {
      ExpectationsModifier modifier = new ExpectationsModifier(loader, classReader, typeMetadata);
      configureClassModifier(modifier);
      return modifier;
   }

   void configureClassModifier(@NotNull ExpectationsModifier modifier) {}

   private void createNewMockInstanceFactoryForInterface()
   {
      Object mock = ConstructorReflection.newInstanceUsingDefaultConstructor(targetClass);
      instanceFactory = new InterfaceInstanceFactory(mock);
   }

   private void generateNewMockImplementationClassForInterface(@NotNull final Type interfaceToMock)
   {
      ImplementationClass<?> implementationGenerator = new ImplementationClass(interfaceToMock) {
         @NotNull @Override
         protected ClassVisitor createMethodBodyGenerator(@NotNull ClassReader typeReader, @NotNull String className)
         {
            return new InterfaceImplementationGenerator(typeReader, interfaceToMock, className);
         }
      };

      targetClass = implementationGenerator.generateNewMockImplementationClassForInterface();
   }

   private void redefinedImplementedInterfacesIfRunningOnJava8(@NotNull Class<?> aClass)
   {
      if (JAVA8) {
         redefineImplementedInterfaces(aClass.getInterfaces());
      }
   }

   final void redefineMethodsAndConstructorsInTargetType()
   {
      redefineClassAndItsSuperClasses(targetClass);
   }

   private void redefineClassAndItsSuperClasses(@NotNull Class<?> realClass)
   {
      ClassLoader loader = realClass.getClassLoader();
      ClassReader classReader = ClassFile.createReaderOrGetFromCache(realClass);
      ExpectationsModifier modifier = createClassModifier(loader, classReader);

      try {
         redefineClass(realClass, classReader, modifier);
      }
      catch (VisitInterruptedException ignore) {
         // As defined in ExpectationsModifier, some critical JRE classes have all methods excluded from mocking by
         // default. This exception occurs when they are visited.
         // In this case, we simply stop class redefinition for the rest of the class hierarchy.
         return;
      }

      redefineElementSubclassesOfEnumTypeIfAny(modifier.enumSubclasses);
      redefinedImplementedInterfacesIfRunningOnJava8(realClass);

      Class<?> superClass = realClass.getSuperclass();

      if (superClass != null && superClass != Object.class && superClass != Proxy.class && superClass != Enum.class) {
         redefineClassAndItsSuperClasses(superClass);
      }
   }

   private void redefineClass(
      @NotNull Class<?> realClass, @NotNull ClassReader classReader, @NotNull ClassVisitor modifier)
   {
      classReader.accept(modifier, SKIP_FRAMES);
      byte[] modifiedClass = modifier.toByteArray();

      applyClassRedefinition(realClass, modifiedClass);
   }

   void applyClassRedefinition(@NotNull Class<?> realClass, @NotNull byte[] modifiedClass)
   {
      ClassDefinition classDefinition = new ClassDefinition(realClass, modifiedClass);
      RedefinitionEngine.redefineClasses(classDefinition);

      if (mockedClassDefinitions != null) {
         mockedClassDefinitions.add(classDefinition);
      }
   }

   private void redefineElementSubclassesOfEnumTypeIfAny(@Nullable List<String> enumSubclasses)
   {
      if (enumSubclasses != null) {
         for (String enumSubclassDesc : enumSubclasses) {
            Class<?> enumSubclass = ClassLoad.loadByInternalName(enumSubclassDesc);
            redefineClass(enumSubclass);
         }
      }
   }

   private void redefineImplementedInterfaces(@NotNull Class<?>[] implementedInterfaces)
   {
      for (Class<?> implementedInterface : implementedInterfaces) {
         redefineClass(implementedInterface);
         redefineImplementedInterfaces(implementedInterface.getInterfaces());
      }
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
      ClassReader classReader = ClassFile.createReaderOrGetFromCache(targetClass);
      String subclassName = getNameForConcreteSubclassToCreate();

      SubclassGenerationModifier modifier =
         new SubclassGenerationModifier(typeMetadata.mockingCfg, typeToMock, classReader, subclassName);
      classReader.accept(modifier, SKIP_FRAMES);
      byte[] bytecode = modifier.toByteArray();

      return ImplementationClass.defineNewClass(targetClass.getClassLoader(), bytecode, subclassName);
   }

   @NotNull
   String getNameForConcreteSubclassToCreate() { return ""; }
}
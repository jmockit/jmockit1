/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.lang.instrument.*;
import java.lang.reflect.*;
import java.lang.reflect.Type;
import java.util.*;
import javax.annotation.*;
import static java.lang.reflect.Modifier.*;

import mockit.external.asm.*;
import mockit.internal.*;
import mockit.internal.classGeneration.*;
import mockit.internal.expectations.mocking.InstanceFactory.*;
import mockit.internal.reflection.*;
import mockit.internal.state.*;
import mockit.internal.util.*;
import mockit.internal.reflection.EmptyProxy.Impl;
import static mockit.external.asm.ClassReader.*;
import static mockit.internal.util.GeneratedClasses.*;
import static mockit.internal.util.Utilities.*;

class BaseTypeRedefinition
{
   private static final class MockedClass
   {
      @Nullable final InstanceFactory instanceFactory;
      @Nonnull final ClassDefinition[] mockedClassDefinitions;

      MockedClass(@Nullable InstanceFactory instanceFactory, @Nonnull ClassDefinition[] classDefinitions)
      {
         this.instanceFactory = instanceFactory;
         mockedClassDefinitions = classDefinitions;
      }

      void redefineClasses()
      {
         RedefinitionEngine.redefineClasses(mockedClassDefinitions);
      }
   }

   @Nonnull private static final Map<Integer, MockedClass> mockedClasses = new HashMap<Integer, MockedClass>();
   @Nonnull private static final Map<Type, Class<?>> mockImplementations = new HashMap<Type, Class<?>>();

   @Nonnull Class<?> targetClass;
   @Nullable MockedType typeMetadata;
   @Nullable private InstanceFactory instanceFactory;
   @Nullable private List<ClassDefinition> mockedClassDefinitions;

   BaseTypeRedefinition() {}

   BaseTypeRedefinition(@Nonnull MockedType typeMetadata)
   {
      targetClass = typeMetadata.getClassType();
      this.typeMetadata = typeMetadata;
   }

   @Nullable
   final InstanceFactory redefineType(@Nonnull Type typeToMock)
   {
      if (targetClass == TypeVariable.class || targetClass.isInterface()) {
         createMockedInterfaceImplementationAndInstanceFactory(typeToMock);
      }
      else {
         if (typeMetadata == null || !typeMetadata.isClassInitializationToBeStubbedOut()) {
            TestRun.ensureThatClassIsInitialized(targetClass);
         }

         redefineTargetClassAndCreateInstanceFactory(typeToMock);
      }

      if (instanceFactory != null) {
         Class<?> mockedType = getClassType(typeToMock);
         TestRun.mockFixture().registerInstanceFactoryForMockedType(mockedType, instanceFactory);
      }

      return instanceFactory;
   }

   private void createMockedInterfaceImplementationAndInstanceFactory(@Nonnull Type interfaceToMock)
   {
      Class<?> mockedInterface = interfaceToMock(interfaceToMock);
      Object mockedInstance;

      if (mockedInterface == null) {
         mockedInstance = createMockInterfaceImplementationUsingStandardProxy(interfaceToMock);
      }
      else {
         mockedInstance = createMockInterfaceImplementationDirectly(interfaceToMock);
      }

      redefinedImplementedInterfacesIfRunningOnJava8(targetClass);
      instanceFactory = new InterfaceInstanceFactory(mockedInstance);
   }

   @Nullable
   private static Class<?> interfaceToMock(@Nonnull Type typeToMock)
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

   @Nonnull
   private Object createMockInterfaceImplementationUsingStandardProxy(@Nonnull Type typeToMock)
   {
      ClassLoader loader = getClass().getClassLoader();
      Object mockedInstance = Impl.newEmptyProxy(loader, typeToMock);
      targetClass = mockedInstance.getClass();
      redefineClass(targetClass);
      return mockedInstance;
   }

   @Nonnull
   private Object createMockInterfaceImplementationDirectly(@Nonnull Type interfaceToMock)
   {
      Class<?> previousMockImplementationClass = mockImplementations.get(interfaceToMock);

      if (previousMockImplementationClass == null) {
         generateNewMockImplementationClassForInterface(interfaceToMock);
         mockImplementations.put(interfaceToMock, targetClass);
      }
      else {
         targetClass = previousMockImplementationClass;
      }

      return ConstructorReflection.newInstanceUsingDefaultConstructor(targetClass);
   }

   private void redefineClass(@Nonnull Class<?> realClass)
   {
      ClassReader classReader = ClassFile.createReaderOrGetFromCache(realClass);

      if (realClass.isInterface() && classReader.getVersion() < Opcodes.V1_8) {
         return;
      }

      ClassLoader loader = realClass.getClassLoader();
      ExpectationsModifier modifier = createClassModifier(loader, classReader);
      redefineClass(realClass, classReader, modifier);
   }

   @Nonnull
   private ExpectationsModifier createClassModifier(@Nonnull ClassLoader loader, @Nonnull ClassReader classReader)
   {
      ExpectationsModifier modifier = new ExpectationsModifier(loader, classReader, typeMetadata);
      configureClassModifier(modifier);
      return modifier;
   }

   void configureClassModifier(@Nonnull ExpectationsModifier modifier) {}

   private void generateNewMockImplementationClassForInterface(@Nonnull final Type interfaceToMock)
   {
      ImplementationClass<?> implementationGenerator = new ImplementationClass(interfaceToMock) {
         @Nonnull @Override
         protected ClassVisitor createMethodBodyGenerator(@Nonnull ClassReader typeReader)
         {
            return new InterfaceImplementationGenerator(typeReader, interfaceToMock, generatedClassName);
         }
      };

      targetClass = implementationGenerator.generateClass();
   }

   private void redefinedImplementedInterfacesIfRunningOnJava8(@Nonnull Class<?> aClass)
   {
      if (JAVA8) {
         redefineImplementedInterfaces(aClass.getInterfaces());
      }
   }

   final boolean redefineMethodsAndConstructorsInTargetType()
   {
      return redefineClassAndItsSuperClasses(targetClass);
   }

   private boolean redefineClassAndItsSuperClasses(@Nonnull Class<?> realClass)
   {
      if (!HOTSPOT_VM && (realClass == System.class || realClass == Object.class)) {
         return false;
      }

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
         return false;
      }

      redefineElementSubclassesOfEnumTypeIfAny(modifier.enumSubclasses);
      redefinedImplementedInterfacesIfRunningOnJava8(realClass);

      Class<?> superClass = realClass.getSuperclass();
      boolean redefined = true;

      if (superClass != null && superClass != Object.class && superClass != Proxy.class && superClass != Enum.class) {
         redefined = redefineClassAndItsSuperClasses(superClass);
      }

      return redefined;
   }

   private void redefineClass(
      @Nonnull Class<?> realClass, @Nonnull ClassReader classReader, @Nonnull ExpectationsModifier modifier)
   {
      classReader.accept(modifier, SKIP_FRAMES);

      if (modifier.wasModified()) {
         byte[] modifiedClass = modifier.toByteArray();
         applyClassRedefinition(realClass, modifiedClass);
      }
   }

   void applyClassRedefinition(@Nonnull Class<?> realClass, @Nonnull byte[] modifiedClass)
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

   private void redefineImplementedInterfaces(@Nonnull Class<?>[] implementedInterfaces)
   {
      for (Class<?> implementedInterface : implementedInterfaces) {
         redefineClass(implementedInterface);
         redefineImplementedInterfaces(implementedInterface.getInterfaces());
      }
   }

   private void redefineTargetClassAndCreateInstanceFactory(@Nonnull Type typeToMock)
   {
      Integer mockedClassId = redefineClassesFromCache();

      if (mockedClassId == null) {
         return;
      }

      boolean redefined = redefineMethodsAndConstructorsInTargetType();
      instanceFactory = createInstanceFactory(typeToMock);

      if (redefined) {
         storeRedefinedClassesInCache(mockedClassId);
      }
   }

   @Nonnull
   protected final InstanceFactory createInstanceFactory(@Nonnull Type typeToMock)
   {
      Class<?> classToInstantiate = targetClass;

      if (isAbstract(classToInstantiate.getModifiers())) {
         classToInstantiate = generateConcreteSubclassForAbstractType(typeToMock);
      }

      return new ClassInstanceFactory(classToInstantiate);
   }

   @Nullable
   private Integer redefineClassesFromCache()
   {
      //noinspection ConstantConditions
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

   private void storeRedefinedClassesInCache(@Nonnull Integer mockedClassId)
   {
      assert mockedClassDefinitions != null;
      ClassDefinition[] classDefs = mockedClassDefinitions.toArray(new ClassDefinition[mockedClassDefinitions.size()]);
      MockedClass mockedClass = new MockedClass(instanceFactory, classDefs);

      mockedClasses.put(mockedClassId, mockedClass);
   }

   @Nonnull
   private Class<?> generateConcreteSubclassForAbstractType(@Nonnull final Type typeToMock)
   {
      final String subclassName = getNameForConcreteSubclassToCreate();

      Class<?> subclass = new ImplementationClass<Object>(targetClass, subclassName) {
         @Nonnull @Override
         protected ClassVisitor createMethodBodyGenerator(@Nonnull ClassReader typeReader)
         {
            return new SubclassGenerationModifier(targetClass, typeToMock, typeReader, subclassName, false);
         }
      }.generateClass();

      return subclass;
   }

   @Nonnull
   private String getNameForConcreteSubclassToCreate()
   {
      String mockId = typeMetadata == null ? null : typeMetadata.getName();
      return getNameForGeneratedClass(targetClass, mockId);
   }
}
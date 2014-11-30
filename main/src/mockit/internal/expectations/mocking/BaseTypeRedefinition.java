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

import mockit.external.asm.*;
import mockit.internal.*;
import mockit.internal.classGeneration.*;
import mockit.internal.expectations.mocking.InstanceFactory.*;
import mockit.internal.state.*;
import mockit.internal.util.*;
import mockit.internal.util.EmptyProxy.Impl;
import static mockit.external.asm.ClassReader.*;
import static mockit.internal.util.GeneratedClasses.*;
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
   @Nullable MockedType typeMetadata;
   @Nullable private InstanceFactory instanceFactory;
   @Nullable private List<ClassDefinition> mockedClassDefinitions;

   BaseTypeRedefinition() {}

   BaseTypeRedefinition(@NotNull MockedType typeMetadata)
   {
      targetClass = typeMetadata.getClassType();
      this.typeMetadata = typeMetadata;
   }

   @Nullable
   final InstanceFactory redefineType(@NotNull Type typeToMock)
   {
      if (targetClass == TypeVariable.class || targetClass.isInterface()) {
         createMockedInterfaceImplementationAndInstanceFactory(typeToMock);
      }
      else {
         redefineTargetClassAndCreateInstanceFactory(typeToMock);
      }

      if (instanceFactory != null) {
         Class<?> mockedType = getClassType(typeToMock);
         TestRun.mockFixture().registerInstanceFactoryForMockedType(mockedType, instanceFactory);
      }

      return instanceFactory;
   }

   private void createMockedInterfaceImplementationAndInstanceFactory(@NotNull Type interfaceToMock)
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

   @NotNull
   private Object createMockInterfaceImplementationUsingStandardProxy(@NotNull Type typeToMock)
   {
      ClassLoader loader = getClass().getClassLoader();
      Object mockedInstance = Impl.newEmptyProxy(loader, typeToMock);
      targetClass = mockedInstance.getClass();
      redefineClass(targetClass);
      return mockedInstance;
   }

   @NotNull
   private Object createMockInterfaceImplementationDirectly(@NotNull Type interfaceToMock)
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

   private void redefineClass(@NotNull Class<?> realClass)
   {
      ClassReader classReader = ClassFile.createReaderOrGetFromCache(realClass);

      if (realClass.isInterface() && classReader.getVersion() < Opcodes.V1_8) {
         return;
      }

      ClassLoader loader = realClass.getClassLoader();
      ExpectationsModifier modifier = createClassModifier(loader, classReader);
      redefineClass(realClass, classReader, modifier);
   }

   @NotNull
   private ExpectationsModifier createClassModifier(@NotNull ClassLoader loader, @NotNull ClassReader classReader)
   {
      ExpectationsModifier modifier = new ExpectationsModifier(loader, classReader, typeMetadata);
      configureClassModifier(modifier);
      return modifier;
   }

   void configureClassModifier(@NotNull ExpectationsModifier modifier) {}

   private void generateNewMockImplementationClassForInterface(@NotNull final Type interfaceToMock)
   {
      ImplementationClass<?> implementationGenerator = new ImplementationClass(interfaceToMock) {
         @NotNull @Override
         protected ClassVisitor createMethodBodyGenerator(@NotNull ClassReader typeReader)
         {
            return new InterfaceImplementationGenerator(typeReader, interfaceToMock, generatedClassName);
         }
      };

      targetClass = implementationGenerator.generateClass();
   }

   private void redefinedImplementedInterfacesIfRunningOnJava8(@NotNull Class<?> aClass)
   {
      if (JAVA8) {
         redefineImplementedInterfaces(aClass.getInterfaces());
      }
   }

   final boolean redefineMethodsAndConstructorsInTargetType()
   {
      return redefineClassAndItsSuperClasses(targetClass);
   }

   private boolean redefineClassAndItsSuperClasses(@NotNull Class<?> realClass)
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
      @NotNull Class<?> realClass, @NotNull ClassReader classReader, @NotNull ExpectationsModifier modifier)
   {
      classReader.accept(modifier, SKIP_FRAMES);

      if (modifier.wasModified()) {
         byte[] modifiedClass = modifier.toByteArray();
         applyClassRedefinition(realClass, modifiedClass);
      }
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

      boolean redefined = redefineMethodsAndConstructorsInTargetType();

      if (redefined) {
         instanceFactory = createInstanceFactory(typeToMock);
         storeRedefinedClassesInCache(mockedClassId);
      }
   }

   @NotNull
   protected final InstanceFactory createInstanceFactory(@NotNull Type typeToMock)
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

   private void storeRedefinedClassesInCache(@NotNull Integer mockedClassId)
   {
      assert mockedClassDefinitions != null;
      ClassDefinition[] classDefs = mockedClassDefinitions.toArray(new ClassDefinition[mockedClassDefinitions.size()]);
      MockedClass mockedClass = new MockedClass(instanceFactory, classDefs);

      mockedClasses.put(mockedClassId, mockedClass);
   }

   @NotNull
   private Class<?> generateConcreteSubclassForAbstractType(@NotNull final Type typeToMock)
   {
      final String subclassName = getNameForConcreteSubclassToCreate();

      Class<?> subclass = new ImplementationClass<Object>(targetClass, subclassName) {
         @NotNull @Override
         protected ClassVisitor createMethodBodyGenerator(@NotNull ClassReader typeReader)
         {
            MockingConfiguration mockingCfg = typeMetadata == null ? null : typeMetadata.mockingCfg;
            return new SubclassGenerationModifier(targetClass, mockingCfg, typeToMock, typeReader, subclassName);
         }
      }.generateClass();

      return subclass;
   }

   @NotNull
   private String getNameForConcreteSubclassToCreate()
   {
      String mockId = typeMetadata == null ? null : typeMetadata.mockId;
      return getNameForGeneratedClass(targetClass, mockId);
   }
}
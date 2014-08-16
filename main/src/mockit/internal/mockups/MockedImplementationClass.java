/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.mockups;

import java.lang.reflect.*;
import java.lang.reflect.Type;
import static java.lang.reflect.Modifier.*;

import mockit.*;
import mockit.external.asm.*;
import mockit.internal.classGeneration.*;
import mockit.internal.util.*;

import org.jetbrains.annotations.*;

public final class MockedImplementationClass<T>
{
   @NotNull private final MockUp<?> mockUpInstance;
   @Nullable private ImplementationClass<T> implementationClass;
   @NotNull private Class<T> generatedClass;

   public MockedImplementationClass(@NotNull MockUp<?> mockUpInstance) { this.mockUpInstance = mockUpInstance; }

   @NotNull
   public Class<T> createImplementation(@NotNull Class<T> interfaceToBeMocked, @Nullable Type typeToMock)
   {
      createImplementation(interfaceToBeMocked);
      byte[] generatedBytecode = implementationClass == null ? null : implementationClass.getGeneratedBytecode();

      MockClassSetup mockClassSetup = new MockClassSetup(generatedClass, typeToMock, mockUpInstance, generatedBytecode);
      mockClassSetup.redefineMethodsInGeneratedClass();

      return generatedClass;
   }

   Class<T> createImplementation(@NotNull Class<T> interfaceToBeMocked)
   {
      if (isPublic(interfaceToBeMocked.getModifiers())) {
         generateImplementationForPublicInterface(interfaceToBeMocked);
      }
      else {
         //noinspection unchecked
         generatedClass = (Class<T>) Proxy.getProxyClass(interfaceToBeMocked.getClassLoader(), interfaceToBeMocked);
      }

      return generatedClass;
   }

   private void generateImplementationForPublicInterface(@NotNull Class<T> interfaceToBeMocked)
   {
      implementationClass = new ImplementationClass<T>(interfaceToBeMocked) {
         @NotNull @Override
         protected ClassVisitor createMethodBodyGenerator(@NotNull ClassReader typeReader)
         {
            return new InterfaceImplementationGenerator(typeReader, generatedClassName);
         }
      };

      generatedClass = implementationClass.generateClass();
   }

   @NotNull
   public Class<T> createImplementation(@NotNull Type[] interfacesToBeMocked)
   {
      Class<?>[] interfacesToMock = new Class<?>[interfacesToBeMocked.length];

      for (int i = 0; i < interfacesToMock.length; i++) {
         interfacesToMock[i] = Utilities.getClassType(interfacesToBeMocked[i]);
      }

      //noinspection unchecked
      generatedClass = (Class<T>) Proxy.getProxyClass(ClassLoader.getSystemClassLoader(), interfacesToMock);
      new MockClassSetup(generatedClass, null, mockUpInstance, null).redefineMethods();

      return generatedClass;
   }
}

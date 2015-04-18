/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.mockups;

import java.lang.reflect.*;
import java.lang.reflect.Type;
import javax.annotation.*;
import static java.lang.reflect.Modifier.*;

import mockit.*;
import mockit.external.asm.*;
import mockit.internal.classGeneration.*;
import mockit.internal.util.*;

public final class MockedImplementationClass<T>
{
   @Nonnull private final MockUp<?> mockUpInstance;
   @Nullable private ImplementationClass<T> implementationClass;
   private Class<T> generatedClass;

   public MockedImplementationClass(@Nonnull MockUp<?> mockUpInstance) { this.mockUpInstance = mockUpInstance; }

   @Nonnull
   public Class<T> createImplementation(@Nonnull Class<T> interfaceToBeMocked, @Nullable Type typeToMock)
   {
      createImplementation(interfaceToBeMocked);
      byte[] generatedBytecode = implementationClass == null ? null : implementationClass.getGeneratedBytecode();

      MockClassSetup mockClassSetup = new MockClassSetup(generatedClass, typeToMock, mockUpInstance, generatedBytecode);
      mockClassSetup.redefineMethodsInGeneratedClass();

      return generatedClass;
   }

   @Nonnull
   Class<T> createImplementation(@Nonnull Class<T> interfaceToBeMocked)
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

   private void generateImplementationForPublicInterface(@Nonnull Class<T> interfaceToBeMocked)
   {
      implementationClass = new ImplementationClass<T>(interfaceToBeMocked) {
         @Nonnull @Override
         protected ClassVisitor createMethodBodyGenerator(@Nonnull ClassReader typeReader)
         {
            return new InterfaceImplementationGenerator(typeReader, generatedClassName);
         }
      };

      generatedClass = implementationClass.generateClass();
   }

   @Nonnull
   public Class<T> createImplementation(@Nonnull Type[] interfacesToBeMocked)
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

/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.transformation;

import java.lang.instrument.*;
import java.security.*;
import java.util.*;
import javax.annotation.*;
import static java.lang.reflect.Modifier.*;

import mockit.external.asm.*;
import mockit.internal.*;
import mockit.internal.startup.*;
import mockit.internal.util.*;
import static mockit.external.asm.ClassReader.*;

public final class ExpectationsTransformer implements ClassFileTransformer
{
   @Nonnull private final List<String> baseSubclasses;

   public ExpectationsTransformer(@Nonnull Instrumentation instrumentation)
   {
      baseSubclasses = new ArrayList<String>();
      baseSubclasses.add("mockit/Expectations");
      baseSubclasses.add("mockit/StrictExpectations");
      baseSubclasses.add("mockit/Verifications");
      baseSubclasses.add("mockit/FullVerifications");
      baseSubclasses.add("mockit/VerificationsInOrder");
      baseSubclasses.add("mockit/FullVerificationsInOrder");

      Class<?>[] alreadyLoaded = instrumentation.getAllLoadedClasses();
      findAndModifyOtherBaseSubclasses(alreadyLoaded);
      modifyFinalSubclasses(alreadyLoaded);
   }

   private void findAndModifyOtherBaseSubclasses(@Nonnull Class<?>[] alreadyLoaded)
   {
      for (Class<?> aClass : alreadyLoaded) {
         if (
            aClass.getClassLoader() != null && !isFinalClass(aClass) &&
            isExpectationsOrVerificationsSubclassFromUserCode(aClass)
         ) {
            modifyInvocationsSubclass(aClass, false);
         }
      }
   }

   private static boolean isFinalClass(@Nonnull Class<?> aClass)
   {
      return isFinal(aClass.getModifiers()) || ClassNaming.isAnonymousClass(aClass);
   }

   private static boolean isExpectationsOrVerificationsSubclassFromUserCode(@Nonnull Class<?> aClass)
   {
      if (isExpectationsOrVerificationsAPIClass(aClass)) {
         return false;
      }

      Class<?> superclass = aClass.getSuperclass();

      while (superclass != null && superclass != Object.class && superclass.getClassLoader() != null) {
         if (isExpectationsOrVerificationsAPIClass(superclass)) {
            return true;
         }

         superclass = superclass.getSuperclass();
      }

      return false;
   }

   private static boolean isExpectationsOrVerificationsAPIClass(@Nonnull Class<?> aClass)
   {
      return
         ("mockit.Expectations mockit.StrictExpectations mockit.Verifications mockit.FullVerifications " +
          "mockit.VerificationsInOrder mockit.FullVerificationsInOrder").contains(aClass.getName());
   }

   private void modifyFinalSubclasses(@Nonnull Class<?>[] alreadyLoaded)
   {
      for (Class<?> aClass : alreadyLoaded) {
         if (
            aClass.getClassLoader() != null && isFinalClass(aClass) &&
            isExpectationsOrVerificationsSubclassFromUserCode(aClass)
         ) {
            modifyInvocationsSubclass(aClass, true);
         }
      }
   }

   private void modifyInvocationsSubclass(@Nonnull Class<?> aClass, boolean isFinalClass)
   {
      ClassReader cr = ClassFile.createClassFileReader(aClass);
      byte[] modifiedClassfile = modifyInvocationsSubclass(cr, aClass.getClassLoader(), isFinalClass);

      if (modifiedClassfile != null) {
         Startup.redefineMethods(aClass, modifiedClassfile);
      }
   }

   @Nullable
   private byte[] modifyInvocationsSubclass(@Nonnull ClassReader cr, ClassLoader loader, boolean finalClass)
   {
      EndOfBlockModifier modifier = new EndOfBlockModifier(cr, loader, finalClass);

      try {
         cr.accept(modifier, SKIP_FRAMES);
         return modifier.toByteArray();
      }
      catch (VisitInterruptedException ignore) {}
      catch (Throwable e) { e.printStackTrace(); }

      return null;
   }

   @Nullable @Override
   public byte[] transform(
      @Nullable ClassLoader loader, @Nonnull String className, @Nullable Class<?> classBeingRedefined,
      @Nullable ProtectionDomain protectionDomain, @Nonnull byte[] classfileBuffer)
   {
      if (classBeingRedefined == null && protectionDomain != null) {
         ClassReader cr = new ClassReader(classfileBuffer);
         String superClassName = cr.getSuperName();

         if (
            !baseSubclasses.contains(superClassName) &&
            !superClassName.endsWith("Expectations") && !superClassName.endsWith("Verifications")
         ) {
            return null;
         }

         boolean finalClass = ClassNaming.isAnonymousClass(className);
         return modifyInvocationsSubclass(cr, loader, finalClass);
      }

      return null;
   }

   private final class EndOfBlockModifier extends ClassVisitor
   {
      @Nonnull private final ClassWriter cw;
      @Nullable private final ClassLoader loader;
      private boolean isFinalClass;
      @Nonnull private String classDesc;

      EndOfBlockModifier(@Nonnull ClassReader cr, @Nullable ClassLoader loader, boolean isFinalClass)
      {
         super(new ClassWriter(cr));
         assert cv != null;
         cw = (ClassWriter) cv;
         this.loader = loader;
         this.isFinalClass = isFinalClass;
         classDesc = "";
      }

      @Override
      public void visit(
         int version, int access, @Nonnull String name, @Nullable String signature, @Nullable String superName,
         @Nullable String[] interfaces)
      {
         if (isFinal(access)) {
            isFinalClass = true;
         }

         if (isClassWhichShouldBeModified(name, superName)) {
            cw.visit(version, access, name, signature, superName, interfaces);
            classDesc = name;
         }
         else {
            throw VisitInterruptedException.INSTANCE;
         }
      }

      private boolean isClassWhichShouldBeModified(@Nonnull String name, @Nullable String superName)
      {
         if (baseSubclasses.contains(name)) {
            return false;
         }

         int i = baseSubclasses.indexOf(superName);
         boolean superClassIsKnownInvocationsSubclass = i >= 0;

         if (isFinalClass) {
            if (superClassIsKnownInvocationsSubclass) {
               return true;
            }

            SuperClassAnalyser superClassAnalyser = new SuperClassAnalyser(loader);

            if (superClassAnalyser.classExtendsInvocationsClass(superName)) {
               return true;
            }
         }
         else if (superClassIsKnownInvocationsSubclass) {
            baseSubclasses.add(name);
            return true;
         }
         else {
            SuperClassAnalyser superClassAnalyser = new SuperClassAnalyser(loader);

            if (superClassAnalyser.classExtendsInvocationsClass(superName)) {
               baseSubclasses.add(name);
               return true;
            }
         }

         return false;
      }

      @Override
      public MethodVisitor visitMethod(
         int access, @Nonnull String name, @Nonnull String desc,
         @Nullable String signature, @Nullable String[] exceptions)
      {
         MethodWriter mw = cw.visitMethod(access, name, desc, signature, exceptions);
         boolean callEndInvocations = isFinalClass && "<init>".equals(name);
         return new InvocationBlockModifier(mw, classDesc, callEndInvocations);
      }
   }

   private final class SuperClassAnalyser extends ClassVisitor
   {
      @Nullable private final ClassLoader loader;
      private boolean classExtendsBaseSubclass;

      private SuperClassAnalyser(@Nullable ClassLoader loader) { this.loader = loader; }

      boolean classExtendsInvocationsClass(@Nullable String classOfInterest)
      {
         if (classOfInterest == null || "java/lang/Object".equals(classOfInterest)) {
            return false;
         }

         ClassReader cr = ClassFile.createClassFileReader(loader, classOfInterest);

         try { cr.accept(this, SKIP_DEBUG + SKIP_FRAMES); } catch (VisitInterruptedException ignore) {}

         return classExtendsBaseSubclass;
      }

      @Override
      public void visit(
         int version, int access, @Nonnull String name, @Nullable String signature, @Nullable String superName,
         @Nullable String[] interfaces)
      {
         classExtendsBaseSubclass = baseSubclasses.contains(superName);

         if (!classExtendsBaseSubclass && !"java/lang/Object".equals(superName)) {
            classExtendsInvocationsClass(superName);
         }

         throw VisitInterruptedException.INSTANCE;
      }
   }
}

/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.transformation;

import java.util.*;
import javax.annotation.*;
import static java.lang.reflect.Modifier.isFinal;

import mockit.external.asm.*;
import mockit.internal.*;
import mockit.internal.util.*;
import static mockit.external.asm.ClassReader.*;

final class EndOfBlockModifier extends ClassVisitor
{
   @Nonnull private final ClassWriter cw;
   @Nullable private final ClassLoader loader;
   @Nonnull private final List<String> baseSubclasses;
   private boolean isFinalClass;
   @Nonnull private String classDesc;

   EndOfBlockModifier(
      @Nonnull ClassReader cr, @Nullable ClassLoader loader, @Nonnull List<String> baseSubclasses, boolean isFinalClass)
   {
      super(new ClassWriter(cr));
      assert cv != null;
      cw = (ClassWriter) cv;
      this.loader = loader;
      this.baseSubclasses = baseSubclasses;
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
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions)
   {
      MethodWriter mw = cw.visitMethod(access, name, desc, signature, exceptions);
      boolean callEndInvocations = isFinalClass && "<init>".equals(name);
      return new InvocationBlockModifier(mw, classDesc, callEndInvocations);
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

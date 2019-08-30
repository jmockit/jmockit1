/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.modification;

import java.util.*;
import javax.annotation.*;

import mockit.asm.classes.*;
import mockit.asm.fields.*;
import mockit.asm.methods.*;
import mockit.coverage.data.*;
import mockit.internal.*;
import static mockit.asm.jvmConstants.Access.*;

final class CoverageModifier extends WrappingClassVisitor
{
   private static final Map<String, CoverageModifier> INNER_CLASS_MODIFIERS = new HashMap<>();
   private static final int FIELD_MODIFIERS_TO_IGNORE = FINAL + SYNTHETIC;

   @Nullable
   static byte[] recoverModifiedByteCodeIfAvailable(@Nonnull String innerClassName) {
      CoverageModifier modifier = INNER_CLASS_MODIFIERS.remove(innerClassName);
      return modifier == null ? null : modifier.toByteArray();
   }

   @Nullable private String internalClassName;
   @Nullable private String simpleClassName;
   @Nonnull private String sourceFileName;
   @Nullable private FileCoverageData fileData;
   private final boolean forInnerClass;
   private boolean forEnumClass;
   @Nullable private String kindOfTopLevelType;

   CoverageModifier(@Nonnull ClassReader cr) { this(cr, false); }

   private CoverageModifier(@Nonnull ClassReader cr, boolean forInnerClass) {
      super(new ClassWriter(cr));
      sourceFileName = "";
      this.forInnerClass = forInnerClass;
   }

   private CoverageModifier(@Nonnull ClassReader cr, @Nonnull CoverageModifier other, @Nullable String simpleClassName) {
      this(cr, true);
      sourceFileName = other.sourceFileName;
      fileData = other.fileData;
      internalClassName = other.internalClassName;
      this.simpleClassName = simpleClassName;
   }

   @Override
   public void visit(int version, int access, @Nonnull String name, @Nonnull ClassInfo additionalInfo) {
      if ((access & SYNTHETIC) != 0) {
         throw new VisitInterruptedException();
      }

      boolean nestedType = name.indexOf('$') > 0;

      if (!nestedType && kindOfTopLevelType == null) {
         //noinspection ConstantConditions
         kindOfTopLevelType = getKindOfJavaType(access, additionalInfo.superName);
      }

      forEnumClass = (access & ENUM) != 0;

      String sourceFileDebugName = getSourceFileDebugName(additionalInfo);

      if (!forInnerClass) {
         extractClassAndSourceFileName(name);

         boolean cannotModify = (access & ANNOTATION) != 0;

         if (cannotModify) {
            throw VisitInterruptedException.INSTANCE;
         }

         registerAsInnerClassModifierIfApplicable(access, name, nestedType);
         createFileData(sourceFileDebugName);
      }

      cw.visit(version, access, name, additionalInfo);
   }

   @Nonnull
   private static String getKindOfJavaType(int typeModifiers, @Nonnull String superName) {
      if ((typeModifiers & ANNOTATION) != 0) return "ant";
      if ((typeModifiers & INTERFACE) != 0) return "itf";
      if ((typeModifiers & ENUM) != 0) return "enm";
      if ((typeModifiers & ABSTRACT) != 0) return "absCls";
      if (superName.endsWith("Exception") || superName.endsWith("Error")) return "exc";
      return "cls";
   }

   @Nonnull
   private static String getSourceFileDebugName(@Nonnull ClassInfo additionalInfo) {
      String sourceFileDebugName = additionalInfo.sourceFileName;

      if (sourceFileDebugName == null || !sourceFileDebugName.endsWith(".java")) {
         throw VisitInterruptedException.INSTANCE;
      }

      return sourceFileDebugName;
   }

   private void extractClassAndSourceFileName(@Nonnull String className) {
      internalClassName = className;
      int p = className.lastIndexOf('/');

      if (p < 0) {
         simpleClassName = className;
         sourceFileName = "";
      }
      else {
         simpleClassName = className.substring(p + 1);
         sourceFileName = className.substring(0, p + 1);
      }
   }

   private void registerAsInnerClassModifierIfApplicable(int access, @Nonnull String name, boolean nestedType) {
      if (!forEnumClass && (access & SUPER) != 0 && nestedType) {
         INNER_CLASS_MODIFIERS.put(name.replace('/', '.'), this);
      }
   }

   private void createFileData(@Nonnull String sourceFileDebugName) {
      sourceFileName += sourceFileDebugName;
      fileData = CoverageData.instance().getOrAddFile(sourceFileName, kindOfTopLevelType);
   }

   @Override
   public void visitInnerClass(@Nonnull String name, @Nullable String outerName, @Nullable String innerName, int access) {
      cw.visitInnerClass(name, outerName, innerName, access);

      if (forInnerClass || isSyntheticOrEnumClass(access) || !isNestedInsideClassBeingModified(name, outerName)) {
         return;
      }

      String innerClassName = name.replace('/', '.');

      if (INNER_CLASS_MODIFIERS.containsKey(innerClassName)) {
         return;
      }

      ClassReader innerCR = ClassFile.createClassReader(CoverageModifier.class.getClassLoader(), name);

      if (innerCR != null) {
         CoverageModifier innerClassModifier = new CoverageModifier(innerCR, this, innerName);
         innerCR.accept(innerClassModifier);
         INNER_CLASS_MODIFIERS.put(innerClassName, innerClassModifier);
      }
   }

   private static boolean isSyntheticOrEnumClass(int access) {
      return (access & SYNTHETIC) != 0 || access == STATIC + ENUM;
   }

   private boolean isNestedInsideClassBeingModified(@Nonnull String internalName, @Nullable String outerName) {
      String className = outerName == null ? internalName : outerName;
      int p = className.indexOf('$');
      String outerClassName = p < 0 ? className : className.substring(0, p);

      return outerClassName.equals(internalClassName);
   }

   @Override
   public FieldVisitor visitField(
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable Object value
   ) {
      if (fileData != null && simpleClassName != null && (access & FIELD_MODIFIERS_TO_IGNORE) == 0) {
         fileData.dataCoverageInfo.addField(simpleClassName, name, (access & STATIC) != 0);
      }

      return cw.visitField(access, name, desc, signature, value);
   }

   @Override
   public MethodVisitor visitMethod(
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions
   ) {
      MethodWriter mw = cw.visitMethod(access, name, desc, signature, exceptions);

      if ((access & SYNTHETIC) != 0 || fileData == null || "<clinit>".equals(name) && forEnumClass) {
         return mw;
      }

      return new MethodModifier(mw, sourceFileName, fileData);
   }
}
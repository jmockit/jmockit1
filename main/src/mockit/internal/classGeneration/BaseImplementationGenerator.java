package mockit.internal.classGeneration;

import java.util.*;

import static java.lang.reflect.Modifier.isStatic;

import mockit.external.asm.*;
import mockit.internal.*;

import static mockit.external.asm.Opcodes.*;

import org.jetbrains.annotations.*;

@SuppressWarnings("AbstractClassExtendsConcreteClass")
public abstract class BaseImplementationGenerator extends BaseClassModifier
{
   private static final int CLASS_ACCESS = ACC_PUBLIC + ACC_FINAL;

   @NotNull private final List<String> implementedMethods;
   @NotNull private final String implementationClassDesc;
   @Nullable private String[] initialSuperInterfaces;

   protected BaseImplementationGenerator(@NotNull ClassReader classReader, @NotNull String implementationClassName)
   {
      super(classReader);
      implementedMethods = new ArrayList<String>();
      implementationClassDesc = implementationClassName.replace('.', '/');
   }

   @Override
   public void visit(
      int version, int access, @NotNull String name, @Nullable String signature, @Nullable String superName,
      @Nullable String[] interfaces)
   {
      initialSuperInterfaces = interfaces;
      String[] implementedInterfaces = {name};
      super.visit(version, CLASS_ACCESS, implementationClassDesc, signature, superName, implementedInterfaces);

      generateNoArgsConstructor();
   }

   private void generateNoArgsConstructor()
   {
      mw = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
      mw.visitVarInsn(ALOAD, 0);
      mw.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
      generateEmptyImplementation();
   }

   @Override
   public final void visitInnerClass(@NotNull String name, String outerName, String innerName, int access) {}

   @Override
   public final void visitOuterClass(@NotNull String owner, @Nullable String name, @Nullable String desc) {}

   @Override
   public final void visitAttribute(Attribute attr) {}

   @Override
   public final void visitSource(@Nullable String source, @Nullable String debug) {}

   @Override @Nullable
   public final FieldVisitor visitField(
      int access, @NotNull String name, @NotNull String desc, @Nullable String signature, @Nullable Object value)
   { return null; }

   @Override @Nullable
   public final MethodVisitor visitMethod(
      int access, @NotNull String name, @NotNull String desc, @Nullable String signature, @Nullable String[] exceptions)
   {
      generateMethodImplementation(access, name, desc, signature, exceptions);
      return null;
   }

   @Override
   public final void visitEnd()
   {
      assert initialSuperInterfaces != null;

      for (String superInterface : initialSuperInterfaces) {
         new MethodGeneratorForImplementedSuperInterface(superInterface);
      }
   }

   protected final void generateMethodImplementation(
      int access, @NotNull String name, @NotNull String desc, @Nullable String signature, @Nullable String[] exceptions)
   {
      if (!isStatic(access)) {
         String methodNameAndDesc = name + desc;

         if (!implementedMethods.contains(methodNameAndDesc)) {
            generateMethodBody(access, name, desc, signature, exceptions);
            implementedMethods.add(methodNameAndDesc);
         }
      }
   }

   protected abstract void generateMethodBody(
      int access, @NotNull String name, @NotNull String desc,
      @Nullable String signature, @Nullable String[] exceptions);

   private final class MethodGeneratorForImplementedSuperInterface extends ClassVisitor
   {
      @Nullable private String[] superInterfaces;

      MethodGeneratorForImplementedSuperInterface(@NotNull String interfaceName)
      {
         ClassFile.visitClass(interfaceName, this);
      }

      @Override
      public void visit(
         int version, int access, @NotNull String name, @Nullable String signature, @Nullable String superName,
         @Nullable String[] interfaces)
      {
         superInterfaces = interfaces;
      }

      @Nullable @Override
      public FieldVisitor visitField(
         int access, @NotNull String name, @NotNull String desc, @Nullable String signature, @Nullable Object value)
      { return null; }

      @Nullable @Override
      public MethodVisitor visitMethod(
         int access, @NotNull String name, @NotNull String desc,
         @Nullable String signature, @Nullable String[] exceptions)
      {
         generateMethodImplementation(access, name, desc, signature, exceptions);
         return null;
      }

      @Override
      public void visitEnd()
      {
         assert superInterfaces != null;

         for (String superInterface : superInterfaces) {
            new MethodGeneratorForImplementedSuperInterface(superInterface);
         }
      }
   }
}

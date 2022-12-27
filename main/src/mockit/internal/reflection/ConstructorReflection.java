/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.reflection;

import static java.lang.reflect.Modifier.isStatic;
import static mockit.internal.reflection.ParameterReflection.NO_PARAMETERS;
import static mockit.internal.reflection.ParameterReflection.acceptsArgumentTypes;
import static mockit.internal.reflection.ParameterReflection.argumentsWithExtraFirstValue;
import static mockit.internal.reflection.ParameterReflection.getArgumentTypesFromArgumentValues;
import static mockit.internal.reflection.ParameterReflection.getParameterTypesDescription;
import static mockit.internal.reflection.ParameterReflection.hasMoreSpecificTypes;
import static mockit.internal.reflection.ParameterReflection.indexOfFirstRealParameter;
import static mockit.internal.reflection.ParameterReflection.invalidArguments;
import static mockit.internal.reflection.ParameterReflection.matchesParameterTypes;
import static mockit.internal.util.Utilities.ensureThatMemberIsAccessible;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import mockit.internal.util.ClassLoad;
import mockit.internal.util.StackTrace;

public final class ConstructorReflection
{

    private static final Constructor<?> OBJECT_CONSTRUCTOR;
    static {
        try {
            OBJECT_CONSTRUCTOR = Object.class.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
    private ConstructorReflection() {}

    @Nonnull
    static <T> Constructor<T> findSpecifiedConstructor(@Nonnull Class<?> theClass, @Nonnull Class<?>[] paramTypes) {
        for (Constructor<?> declaredConstructor : theClass.getDeclaredConstructors()) {
            Class<?>[] declaredParameterTypes = declaredConstructor.getParameterTypes();
            int firstRealParameter = indexOfFirstRealParameter(declaredParameterTypes, paramTypes);

            if (firstRealParameter >= 0 && matchesParameterTypes(declaredParameterTypes, paramTypes, firstRealParameter)) {
                //noinspection unchecked
                return (Constructor<T>) declaredConstructor;
            }
        }

        String paramTypesDesc = getParameterTypesDescription(paramTypes);

        throw new IllegalArgumentException("Specified constructor not found: " + theClass.getSimpleName() + paramTypesDesc);
    }

    @Nonnull
    public static <T> T invokeAccessible(@Nonnull Constructor<T> constructor, @Nonnull Object... initArgs) {
        try {
            return constructor.newInstance(initArgs);
        }
        catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        catch (InvocationTargetException e) {
            Throwable cause = e.getCause();

            if (cause instanceof Error) {
                throw (Error) cause;
            }
            else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            else {
                ThrowOfCheckedException.doThrow((Exception) cause);
                throw new IllegalStateException("Should never get here", cause);
            }
        }
    }

    public static void newInstanceUsingCompatibleConstructor(@Nonnull Class<?> aClass, @Nonnull String argument) throws ReflectiveOperationException {
        Constructor<?> constructor = aClass.getDeclaredConstructor(String.class);
        ensureThatMemberIsAccessible(constructor);
        constructor.newInstance(argument);
    }

    @Nonnull
    public static <T> T newInstanceUsingDefaultConstructor(@Nonnull Class<T> aClass) {
        try {
            Constructor<T> constructor = aClass.getDeclaredConstructor();
            ensureThatMemberIsAccessible(constructor);
            return constructor.newInstance();
        }
        catch (NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        catch (InvocationTargetException e) {
            throw new RuntimeException(e.getTargetException());
        }
    }

    @Nullable
    public static <T> T newInstanceUsingDefaultConstructorIfAvailable(@Nonnull Class<T> aClass) {
        try {
            Constructor<T> constructor = aClass.getDeclaredConstructor();
            return constructor.newInstance();
        }
        catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ignore) { return null; }
    }

    @Nullable
    public static <T> T newInstanceUsingPublicConstructorIfAvailable(
                                                                     @Nonnull Class<T> aClass, @Nonnull Class<?>[] parameterTypes, @Nonnull Object... initArgs
            ) {
        Constructor<T> publicConstructor;
        try { publicConstructor = aClass.getConstructor(parameterTypes); }
        catch (NoSuchMethodException ignore) { return null; }

        return invokeAccessible(publicConstructor, initArgs);
    }

    @Nonnull
    public static <T> T newInstance(@Nonnull Class<T> aClass, @Nonnull Class<?>[] parameterTypes,
                                    @Nullable Object... initArgs) {
        if (initArgs == null) {
            throw invalidArguments();
        }

        Constructor<T> constructor = findSpecifiedConstructor(aClass, parameterTypes);
        return invoke(constructor, initArgs);
    }

    @Nonnull
    public static <T> T invoke(@Nonnull Constructor<T> constructor, @Nonnull Object... initArgs) {
        ensureThatMemberIsAccessible(constructor);

        try {
            return constructor.newInstance(initArgs);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();

            if (cause instanceof Error) {
                throw (Error) cause;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                ThrowOfCheckedException.doThrow((Exception) cause);
                throw new IllegalStateException("Should never get here", cause);
            }
        }
    }

    @Nonnull
    public static <T> T newInstance(@Nonnull String className, @Nonnull Class<?>[] parameterTypes,
                                    @Nullable Object... initArgs) {
        Class<T> theClass = ClassLoad.loadClass(className);
        return newInstance(theClass, parameterTypes, initArgs);
    }

    @Nonnull
    public static <T> T newInstance(@Nonnull String className, @Nullable Object... nonNullArgs) {
        if (nonNullArgs == null) {
            throw invalidArguments();
        }

        Class<?>[] argTypes = getArgumentTypesFromArgumentValues(nonNullArgs);
        Class<T> theClass = ClassLoad.loadClass(className);
        Constructor<T> constructor = findCompatibleConstructor(theClass, argTypes);
        return invoke(constructor, nonNullArgs);
    }

    @Nonnull
    private static <T> Constructor<T> findCompatibleConstructor(@Nonnull Class<?> theClass,
                                                                @Nonnull Class<?>[] argTypes) {
        Constructor<T> found = null;
        Class<?>[] foundParameters = null;
        Constructor<?>[] declaredConstructors = theClass.getDeclaredConstructors();

        for (Constructor<?> declaredConstructor : declaredConstructors) {
            Class<?>[] declaredParamTypes = declaredConstructor.getParameterTypes();
            int firstRealParameter = indexOfFirstRealParameter(declaredParamTypes, argTypes);

            if (firstRealParameter >= 0
                    && (matchesParameterTypes(declaredParamTypes, argTypes, firstRealParameter)
                            || acceptsArgumentTypes(declaredParamTypes, argTypes, firstRealParameter))
                    && (found == null || hasMoreSpecificTypes(declaredParamTypes, foundParameters))) {
                //noinspection unchecked
                found = (Constructor<T>) declaredConstructor;
                foundParameters = declaredParamTypes;
            }
        }

        if (found != null) {
            return found;
        }

        Class<?> declaringClass = theClass.getDeclaringClass();
        Class<?>[] paramTypes = declaredConstructors[0].getParameterTypes();

        if (paramTypes.length > argTypes.length && paramTypes[0] == declaringClass) {
            throw new IllegalArgumentException("Invalid instantiation of inner class; use newInnerInstance instead");
        }

        String argTypesDesc = getParameterTypesDescription(argTypes);
        throw new IllegalArgumentException(
                "No compatible constructor found: " + theClass.getSimpleName() + argTypesDesc);
    }

    @Nonnull
    public static <T> T newInstance(@Nonnull Class<? extends T> aClass, @Nullable Object... nonNullArgs) {
        if (nonNullArgs == null) {
            throw invalidArguments();
        }

        Class<?>[] argTypes = getArgumentTypesFromArgumentValues(nonNullArgs);
        Constructor<T> constructor = findCompatibleConstructor(aClass, argTypes);
        return invoke(constructor, nonNullArgs);
    }

    @Nonnull
    public static <T> T newInstanceUsingDefaultConstructor2(@Nonnull Class<T> aClass) {
        try {
            //noinspection ClassNewInstance
            return aClass.newInstance();
        } catch (InstantiationException ie) {
            throw new RuntimeException(ie);
        } catch (IllegalAccessException ignore) {
            return newInstance(aClass, (Object[]) NO_PARAMETERS);
        }
    }

    @Nullable
    public static <T> T newInstanceUsingDefaultConstructorIfAvailable2(@Nonnull Class<T> aClass) {
        try {
            //noinspection ClassNewInstance
            return aClass.newInstance();
        } catch (InstantiationException ignore) {
            return null;
        } catch (IllegalAccessException ignore) {
            return null;
        }
    }

    @Nullable
    public static <T> T newInstanceUsingPublicConstructorIfAvailable2(@Nonnull Class<T> aClass,
                                                                      @Nonnull Class<?>[] parameterTypes,
                                                                      @Nonnull Object... initArgs) {
        Constructor<T> publicConstructor;
        try {
            publicConstructor = aClass.getConstructor(parameterTypes);
        } catch (NoSuchMethodException ignore) {
            return null;
        }

        return invoke(publicConstructor, initArgs);
    }

    @Nonnull
    public static <T> T newInnerInstance(@Nonnull Class<? extends T> innerClass, @Nonnull Object outerInstance,
                                         @Nullable Object... nonNullArgs) {
        if (nonNullArgs == null) {
            throw invalidArguments();
        }

        if (isStatic(innerClass.getModifiers())) {
            throw new IllegalArgumentException(innerClass.getSimpleName() + " is not an inner class");
        }

        Object[] initArgs = argumentsWithExtraFirstValue(nonNullArgs, outerInstance);
        return newInstance(innerClass, initArgs);
    }

    @Nonnull
    public static <T> T newInnerInstance(@Nonnull String innerClassName, @Nonnull Object outerInstance,
                                         @Nullable Object... nonNullArgs) {
        Class<?> outerClass = outerInstance.getClass();
        ClassLoader loader = outerClass.getClassLoader();
        String className = outerClass.getName() + '$' + innerClassName;
        Class<T> innerClass = ClassLoad.loadFromLoader(loader, className);

        return newInnerInstance(innerClass, outerInstance, nonNullArgs);
    }

    @SuppressWarnings({ "UnnecessaryFullyQualifiedName", "UseOfSunClasses" })
    private static final sun.reflect.ReflectionFactory REFLECTION_FACTORY = sun.reflect.ReflectionFactory
    .getReflectionFactory();

    @Nonnull
    public static <T> T newUninitializedInstance(@Nonnull Class<T> aClass) {

        try {

            Constructor<?> fakeConstructor = REFLECTION_FACTORY.newConstructorForSerialization(aClass,
                    OBJECT_CONSTRUCTOR);

            if (fakeConstructor == null) { // can happen on Java 9
                //noinspection ConstantConditions
                return null;
            }

            @SuppressWarnings("unchecked")
            T newInstance = (T) fakeConstructor.newInstance();
            return newInstance;
        } catch (NoClassDefFoundError e) {
            StackTrace.filterStackTrace(e);
            e.printStackTrace();
            throw e;
        } catch (ExceptionInInitializerError e) {
            StackTrace.filterStackTrace(e);
            e.printStackTrace();
            throw e;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
    }
}

package ru.ifmo.ctddev.koroleva.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;


/**
 * This class implements interface {@link info.kgeorgiy.java.advanced.implementor.JarImpler}. It could create implementations
 * of given class or interface and write it to .jar-file.
 *
 * @author Yana Koroleva
 */
public class Implementor implements JarImpler {

    /**
     * Get as arguments flag "-jar", name of class, which
     * should be implemented, and name of jar file to write.
     * It calls function {@link #implementJar}
     *
     * @param args arguments for function. Should be 3.
     */
    public static void main(String[] args) {
        if (args == null) {
            throw new NullPointerException();
        } else if (args.length != 3) {
            System.out.println("Need 3 arguments. Have only " + args.length + ".");
        } else if (!args[0].equals("-jar")) {
            System.out.println("Wrong arguments.");
        } else {
            try {
                new Implementor().implementJar(Class.forName(args[1]), new File(args[2]));
            } catch (ImplerException e) {
                System.out.println("Impler exception caught.");
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                System.out.println("Class not found.");
            }
        }
    }


    /**
     * Implements given class, compile the result and
     * write .class at .jar-file. Calls {@link #implement}
     *
     * @param aClass class, which implementaion function should be created.
     * @param file   .jar-file, where function have to write result.
     * @throws ImplerException if problems with creating implementation
     *                         or writing to .jar-file.
     */
    @Override
    public void implementJar(Class<?> aClass, File file) throws ImplerException {

        Path path;
        try {
            path = Files.createTempDirectory("Impl");
        } catch (IOException e) {
            throw new ImplerException("Problems with creating temp directory.");
        }
        implement(aClass, path.toFile());

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Could not find java compiler, include tools.jar to classpath");
        }

        final List<String> args = new ArrayList<>();
        Package pack = aClass.getPackage();
        args.add("-cp");
        args.add(System.getProperty("java.class.path"));
        args.add(path.toString() + (pack == null ? "" : (File.separator + pack.getName().replace(".", File.separator)))
                + File.separator + aClass.getSimpleName() + "Impl.java");

        final int exitCode = compiler.run(null, null, null, args.toArray(new String[args.size()]));
        if (exitCode != 0) {
            throw new ImplerException("Compiler exit code " + exitCode);
        }

        try (JarOutputStream jarOutput = new JarOutputStream(new FileOutputStream(file))) {
            jarOutput.putNextEntry(new JarEntry((pack == null ? "" : (pack.getName().replace(".", "/")))
                    + "/" + aClass.getSimpleName() + "Impl.class"));

            Files.copy(path.resolve((pack == null ? "" : (pack.getName().replace(".", File.separator)))
                    + File.separator + aClass.getSimpleName() + "Impl.class"), jarOutput);
        } catch (IOException e) {
            throw new ImplerException("Problems with jarOutputStream");
        }
    }

    /**
     * Creates directories from given class package and create
     * implementation for it.
     *
     * @param myClass class, which implementation should be created.
     * @param file    path, where the implementation should be created.
     * @throws ImplerException if null myClass or file or class is primitive type or final,
     *                         or if there are problems with writing implementation.
     *
     * @see #implementClass implementClass
     * @see #implementInterface implementInterface
     */
    @Override
    public void implement(Class<?> myClass, File file) throws ImplerException {
        if (myClass == null) {
            throw new ImplerException("Null input class.");
        }
        if (file == null) {
            throw new ImplerException("Null output file.");
        }
        if (myClass.isPrimitive() || Modifier.isFinal(myClass.getModifiers())) {
            throw new ImplerException("Cannot implement this class.");
        }

        String myClassName = myClass.getSimpleName();

        String pack = myClass.getPackage().getName();
        File directories = new File(file, pack.replace(".", File.separator));
        directories.mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(new File(directories, myClassName + "Impl.java"))))) {
            writer.append("package " + pack + ";\n\n");
            if (myClass.isInterface()) {
                writer.append("public class " + myClassName + "Impl implements " + myClassName + " {\n");
                implementInterface(writer, myClass);
            } else {
                writer.append("public class " + myClassName + "Impl extends " + myClassName + " {\n");
                implementClass(writer, myClass);
            }

            writer.append("}");
        } catch (FileNotFoundException e) {
            throw new ImplerException("Problems with creating output file.");
        } catch (IOException e) {
            throw new ImplerException("Problems with writing to file.");
        }
    }


    /**
     * Writes class implementation to writer's file.
     *
     * @param writer  BufferedWriter, where to write.
     * @param myClass class, which implementation should be created.
     * @throws ImplerException if problems with finding not private constructor.
     * @throws java.io.IOException if problems with writing to file.
     *
     * @see #implementMethods implementMethods
     * @see #implementConstructors implementConstructors
     */
    private static void implementClass(BufferedWriter writer, Class myClass)
            throws ImplerException, IOException {
        HashSet<String> usedMethods = new HashSet<>();
        implementConstructors(writer, myClass);
        implementMethods(writer, myClass.getMethods(), usedMethods);
        while (myClass != null) {
            implementMethods(writer, myClass.getDeclaredMethods(), usedMethods);
            myClass = myClass.getSuperclass();
        }
    }

    /**
     * Writes interface implementation to writer's file.
     *
     * @param writer      BufferedWriter, where to write.
     * @param myInterface interface, which implementation should be created.
     * @throws java.io.IOException if problems with writing to file.
     *
     * @see #implementMethods implementMethods
     */
    private static void implementInterface(BufferedWriter writer, Class myInterface)
            throws IOException {
        HashSet<String> usedMethods = new HashSet<>();
        implementMethods(writer, myInterface.getMethods(), usedMethods);
    }

    /**
     * Writes first not private class constructor implementation to writer's file.
     *
     * @param writer  BufferedWriter, where to write.
     * @param myClass class, which constructor should be implemented.
     * @throws ImplerException if there is no not private constructors.
     * @throws java.io.IOException if problems with writing to file.
     *
     * @see #implementConstructor implementConstructor
     */
    private static void implementConstructors(BufferedWriter writer, Class myClass)
            throws ImplerException, IOException {
        Constructor[] constructors = myClass.getDeclaredConstructors();
        if (constructors.length == 0) {
            throw new ImplerException();
        } else {
            for (Constructor i : constructors) {
                if (!Modifier.isPrivate(i.getModifiers())) {
                    implementConstructor(writer, i, myClass.getSimpleName());
                    return;
                }
            }
            throw new ImplerException();
        }

    }

    /**
     * Writes to writer's file default implementation of given constructor.
     *
     * @param writer      BufferedWriter, where to write.
     * @param constructor constructor, which implementation should be written.
     * @param myClassName name of class, which constructor is.
     * @throws java.io.IOException if problems with writing to file.
     *
     * @see #parametersToString parametersToString
     */
    private static void implementConstructor(BufferedWriter writer, Constructor constructor, String myClassName)
            throws IOException {
        writer.append("    " + Modifier.toString(constructor.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT) + " ");
        writer.append(myClassName + "Impl(");
        Parameter[] parameters = constructor.getParameters();
        writer.append(parametersToString(parameters, true).toString() + ")");

        Class[] exceptions = constructor.getExceptionTypes();
        if (exceptions.length > 0) {
            writer.append(" throws ");
            for (Class i : exceptions) {
                writer.append(i.getCanonicalName());
                if (i != exceptions[exceptions.length - 1]) {
                    writer.append(", ");
                }
            }
        }

        writer.append("{\n        super(");
        writer.append(parametersToString(parameters, false).toString());

        writer.append(");\n    }\n\n");
    }

    /**
     * Writes default implementation of given methods to writer's file.
     *
     * @param writer      BufferedWriter, where to write.
     * @param methods     methods, which should be realised.
     * @param usedMethods methods, that have already been realised.
     * @throws java.io.IOException if problems with writing to file
     *
     * @see #parametersToString parametersToString
     */
    private static void implementMethods(BufferedWriter writer, Method[] methods, HashSet<String> usedMethods) throws IOException {
        for (Method i : methods) {
            String methodName = i.getName() + "(";

            int modifiers = i.getModifiers();
            Parameter[] parameters = i.getParameters();
            methodName += parametersToString(parameters, true).toString();
            methodName += ")";

            if (!Modifier.isAbstract(modifiers) || !usedMethods.add(methodName)) {
                writer.append("    @Override\n    ");
                writer.append(Modifier.toString(modifiers & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT)
                        + " " + i.getReturnType().getCanonicalName() + " ");
                writer.append(methodName + " {\n");

                if (Object.class.isAssignableFrom(i.getReturnType())) {
                    writer.append("        return null;\n");
                } else if (i.getReturnType().equals(boolean.class)) {
                    writer.append("        return false;\n");
                } else if (i.getReturnType().equals(void.class)) {
                    writer.append("        return;\n");
                } else {
                    writer.append("        return 0;\n");
                }
                writer.append("    }\n\n");
            }
        }
    }

    /**
     * Write parameters with its' type or without to string builder.
     *
     * @param parameters parameters, which should be written.
     * @param withType   flag, that should be true, to write parameters with type, and false otherwise.
     * @return string builder with parameters.
     */
    private static StringBuilder parametersToString(Parameter[] parameters, boolean withType) {
        StringBuilder result = new StringBuilder();
        int cnt = 0;
        for (Parameter i : parameters) {
            result.append(withType ? i.getType().getCanonicalName() : "");
            result.append(" arg" + cnt++);
            if (i != parameters[parameters.length - 1]) {
                result.append(", ");
            }
        }
        return result;
    }
}

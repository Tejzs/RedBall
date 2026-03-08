package redball.engine.utils;

import redball.engine.core.Engine;
import redball.engine.entity.ECSWorld;
import redball.engine.entity.GameObject;
import redball.engine.entity.components.Component;
import redball.engine.scene.AssetManager;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ScriptManager implements Runnable {
    private static final Map<String, ClassLoader> loaderMap = new ConcurrentHashMap<>();
    private static final Map<String, Class<?>> classMap = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<File> reloadQueue = new ConcurrentLinkedQueue<>();
    private static final String OUTPUT_DIR = ScriptManager.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    public static volatile boolean reloaded = false;
    private static int res = 0;

    @Override
    public void run() {
        try {
            FolderObserver.start();
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    public static boolean compileAll(String scriptsDir) throws Exception {
        if (Engine.isBuild) {
            loadAllFromPak();
            return true;
        }

        File dir = new File(scriptsDir);
        File[] javaFiles = dir.listFiles((f, name) -> name.endsWith(".java"));
        if (javaFiles == null || javaFiles.length == 0) return false;
        for (File file : javaFiles) {
            compile(file);
        }
        if (res != 0) {
            res = 0;
            return false;
        }
        return true;
    }

    private static void loadAllFromPak() throws Exception {
        for (String key : PakWriter.getManifestFile().keySet()) {
            if (!key.endsWith(".class")) continue;

            String fullName = key
                    .replaceAll("^.*?out/", "")
                    .replace("/", ".")
                    .replace(".class", "");

            ClassLoader old = loaderMap.get(fullName);
            if (old instanceof URLClassLoader ucl) ucl.close();

            String finalFullName = fullName;
            ClassLoader loader = new ClassLoader(ScriptManager.class.getClassLoader()) {
                @Override
                protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                    if (name.startsWith("redball.example.assets.scripts.")) {
                        try {
                            Class<?> c = findClass(name);
                            if (resolve) resolveClass(c);
                            return c;
                        } catch (ClassNotFoundException ignored) {}
                    }
                    return super.loadClass(name, resolve);
                }

                @Override
                protected Class<?> findClass(String name) throws ClassNotFoundException {
                    String classFile = name.replace(".", "/") + ".class";
                    String pakPath = PakWriter.getManifestFile().entrySet().stream()
                            .filter(e -> e.getKey().endsWith(classFile))
                            .map(Map.Entry::getValue)
                            .findFirst()
                            .orElseThrow(() -> new ClassNotFoundException(name));
                    try {
                        byte[] bytes = new FileInputStream(pakPath).readAllBytes();
                        return defineClass(name, bytes, 0, bytes.length);
                    } catch (IOException e) {
                        throw new ClassNotFoundException(name, e);
                    }
                }
            };

            loaderMap.put(fullName, loader);
            Class<?> clazz = loader.loadClass(fullName);
            classMap.put(fullName, clazz);
        }
    }

    public static Class<?> compile(File file) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new RuntimeException("No compiler — use JDK not JRE");

        new File(AssetManager.getINSTANCE().getCompileDirectory()).mkdirs();
        int result = compiler.run(
                null, System.out, System.err,
                "-classpath", OUTPUT_DIR + File.pathSeparator + System.getProperty("java.class.path"),
                "-sourcepath", "",
                "-proc:none",
                "-d", AssetManager.getINSTANCE().getCompileDirectory(),
                file.getPath()
        );
        if (res == 0) {
            res = result;
        }
        if (result != 0) {
            System.err.println("Compilation failed — check stderr above");
        }

        String fullName = getFullyQualifiedName(file);

        ClassLoader old = loaderMap.get(fullName);
        if (old instanceof URLClassLoader ucl) ucl.close();

        URLClassLoader loader = new URLClassLoader(new URL[]{new File(AssetManager.getINSTANCE().getCompileDirectory()).toURI().toURL()}, ScriptManager.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("redball.example.assets.scripts.")) {
                    try {
                        Class<?> c = findClass(name);
                        if (resolve) resolveClass(c);
                        return c;
                    } catch (ClassNotFoundException ignored) {}
                }
                return super.loadClass(name, resolve);
            }
        };

        loaderMap.put(fullName, loader);
        Class<?> clazz = loader.loadClass(fullName);
        classMap.put(fullName, clazz);
        return clazz;
    }

    public static void scheduleReload(File file) {
        reloadQueue.add(file);
    }

    public static void processReloads() {
        File file;
        while ((file = reloadQueue.poll()) != null) {
            try {
                reloadDomain(file);
            } catch (Exception e) {
                System.err.println("Reload failed for " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    private static void reloadDomain(File file) throws Exception {
        Class<?> clazz = compile(file);

        for (GameObject go : ECSWorld.getGameObjects()) {
            for (Component c : new ArrayList<>(go.getComponents())) {
                if (c.getClass().getName().equals(clazz.getName())) {
                    Component newInstance = (Component) clazz.getDeclaredConstructor().newInstance();
                    go.removeComponentByName(clazz.getName());
                    go.addComponent(newInstance);
                    break;
                }
            }
        }
        reloaded = true;
    }

    public static String getFullyQualifiedName(File file) throws IOException {
        String fileName = file.getName().replace(".java", "");
        String fileContent = Files.readString(file.toPath());

        Pattern packagePattern = Pattern.compile("^\\s*package\\s+([^;]+);", Pattern.MULTILINE);
        Matcher matcher = packagePattern.matcher(fileContent);

        if (matcher.find()) {
            return matcher.group(1).trim() + "." + fileName;
        }
        return fileName;
    }

    public static ClassLoader getScriptClassLoader(String className) {
        ClassLoader loader = loaderMap.get(className);
        return loader != null ? loader : ScriptManager.class.getClassLoader();
    }

    public static Map<String, Class<?>> getClassMap() {
        return classMap;
    }
}
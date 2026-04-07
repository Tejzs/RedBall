package redball.engine.utils;

import redball.engine.core.Engine;
import redball.engine.editor.EditorLayer;
import redball.engine.entity.ECSWorld;
import redball.engine.entity.GameObject;
import redball.engine.entity.components.Component;
import redball.engine.scene.AssetManager;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptManager implements Runnable {
    private static final Map<String, ClassLoader> loaderMap = new ConcurrentHashMap<>();
    private static final Map<String, Class<?>> classMap = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<File> reloadQueue = new ConcurrentLinkedQueue<>();
    private static final String OUTPUT_DIR = ScriptManager.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    private static volatile boolean reloaded = false;
    private static int errorCount = 0;

    @Override
    public void run() {
        try {
            FolderObserver.start();
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    public static void compileAll(String scriptsDir) throws Exception {
        if (Engine.isBuild) {
            loadAllFromPak();
        }

        File dir = new File(scriptsDir);
        File[] javaFiles = dir.listFiles((f, name) -> name.endsWith(".java"));
        if (javaFiles == null || javaFiles.length == 0) return;
        for (File file : javaFiles) {
            compile(file);
        }
    }

    private static void loadAllFromPak() throws Exception {
        // only get the index (names), not the actual bytes
        Set<String> keys = PakWriter.getIndex().keySet();

        for (String key : keys) {
            if (!key.endsWith(".class")) continue;

            String fullName = key
                    .replaceAll("^.*?out/", "")
                    .replace("/", ".")
                    .replace(".class", "");

            ClassLoader old = loaderMap.get(fullName);
            if (old instanceof URLClassLoader ucl) ucl.close();

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

                    // find the matching key in index
                    for (String k : PakWriter.getIndex().keySet()) {
                        if (k.endsWith(classFile)) {
                            return defineClass(name, PakWriter.getAsset(k), 0, PakWriter.getAsset(k).length);
                        }
                    }
                    throw new ClassNotFoundException(name);
                }
            };

            loaderMap.put(fullName, loader);
            System.out.println(fullName);
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
        if (result == 0) {
            EditorLayer.setCompileSuccess(true);
            errorCount = 0;
        }
        if (result != 0) {
            System.err.println("Compilation failed — check stderr above");
            EditorLayer.setCompileSuccess(false);
            errorCount++;
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

    public static int getErrorCount() {
        return errorCount;
    }
}
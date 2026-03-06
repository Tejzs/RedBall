package redball.engine.utils;

import redball.engine.entity.ECSWorld;
import redball.engine.entity.GameObject;
import redball.engine.entity.components.Component;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptManager implements Runnable {
    private static final Map<String, URLClassLoader> loaderMap = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<File> reloadQueue = new ConcurrentLinkedQueue<>();
    private static final String OUTPUT_DIR = ScriptManager.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    private static final String RELOAD_DIR = "example/out/";
    public static volatile boolean reloaded = false;

    @Override
    public void run() {
        try {
            FolderObserver.start();
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    public static void compileAll(String scriptsDir) throws Exception {
        File dir = new File(scriptsDir);
        File[] javaFiles = dir.listFiles((f, name) -> name.endsWith(".java"));
        if (javaFiles == null || javaFiles.length == 0) return;

        for (File file : javaFiles) {
            compile(file);
        }
    }

    public static Class<?> compile(File file) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new RuntimeException("No compiler — use JDK not JRE");

        new File(RELOAD_DIR).mkdirs();

        int result = compiler.run(null, System.out, System.err, "-classpath", OUTPUT_DIR + File.pathSeparator + System.getProperty("java.class.path"), "-d", RELOAD_DIR, file.getPath());
        if (result != 0) throw new RuntimeException("Compilation failed — check stderr above");

        String fullName = getFullyQualifiedName(file);

        URLClassLoader old = loaderMap.get(fullName);
        if (old != null) old.close();

        URLClassLoader loader = new URLClassLoader(new URL[]{new File(RELOAD_DIR).toURI().toURL()}, ScriptManager.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("redball.example.assets.scripts.")) {
                    try {
                        Class<?> c = findClass(name);
                        if (resolve) resolveClass(c);
                        return c;
                    } catch (ClassNotFoundException ignored) {
                    }
                }
                return super.loadClass(name, resolve);
            }
        };

        loaderMap.put(fullName, loader);

        Class<?> clazz = loader.loadClass(fullName);
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
                    Component result = go.addComponent(newInstance);
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
        URLClassLoader loader = loaderMap.get(className);
        return loader != null ? loader : ScriptManager.class.getClassLoader();
    }
}
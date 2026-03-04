package redball.engine.core;

import redball.engine.save.SaveManager;

import java.io.*;
import java.lang.reflect.InvocationTargetException;

public class AssetManager {
    private static AssetManager INSTANCE;
    private static String workingDirectory;
    public static String scenesDirectory;
    private static File file;

    public AssetManager(String directory) {
        workingDirectory = directory;
        scenesDirectory = workingDirectory + File.separatorChar + "assets/scenes" + File.separatorChar;
        file = new File(workingDirectory);
    }

    public void saveScene() {
        SaveManager.save();
    }

    public void loadScene() {
        SaveManager.loadScene();
    }

    public static String getScenesDirectory() {
        return scenesDirectory;
    }

    public static String getWorkingDirectory() {
        return workingDirectory;
    }

    public static void setWorkingDirectory(String workingDirectory) {
        AssetManager.workingDirectory = workingDirectory;
    }

    public static void init(String workingDirectory) {
        INSTANCE = new AssetManager(workingDirectory);
    }

    public static AssetManager getINSTANCE() {
        return INSTANCE;
    }

    public static File getFile() {
        return file;
    }

    public static void setFile(File file) {
        AssetManager.file = file;
    }
}